package me.artificial.autoserver.common;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BootListener {
    private final static int CLIENT_TIMEOUT = 5000;
    private final static int SERVER_RETRIES = 10;
    private final static int DELAY_BETWEEN_RETIRES = 5000; // 5 seconds

    private final ExecutorService clientHandlers = Executors.newCachedThreadPool();
    private Integer port = null;
    private BackendConfig config = null;
    private ServerSocket serverSocket = null;
    private volatile boolean running = true;

    public static void main(String[] args) {
        BootListener bootListener = new BootListener();
        bootListener.start();
    }

    private void start() {
        if (!initialize()) return;

        if (!setupServerSocket()) return;

        sendBannerMessage();

        new Thread(this::startSocketServer).start();

        // Start loop for user cli input
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (running) {
            System.out.print("> ");
            try {
                String command = r.readLine();
                processCliCommand(command);
            } catch (IOException ignored) {
            }
        }

        cleanup();
    }

    private boolean initialize() {
        String path;
        try {
            path = new File(BootListener.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        } catch (URISyntaxException e) {
            System.err.println("Unable to resolve the file path for the config directory.");
            return false;
        }
        this.config = new BackendConfig(new File(path, BackendConfig.DIRECTORY_NAME_DEFAULT));
        this.port = config.getInt("bootListener.port");
        if (port == null) {
            System.err.println("Missing required setting \"bootListener.port\". Please add and restart.");
            return false;
        }
        return true;
    }

    private boolean setupServerSocket() {
        int retries = SERVER_RETRIES;
        while (running && retries-- > 0) {
            try {
                assert port != null;
                serverSocket = new ServerSocket(port);
                return true;
            } catch (IOException e) {
                System.out.println("Error starting server: " + e.getMessage());
            }

            System.out.println("Retrying to start server in " + (DELAY_BETWEEN_RETIRES / 1000) + " seconds...");
            try {
                Thread.sleep(DELAY_BETWEEN_RETIRES);
            } catch (InterruptedException e) {
                System.out.println("Retry delay interrupted: " + e.getMessage());
                Thread.currentThread().interrupt(); // Keep the interrupt going
            }
        }
        System.err.println("Failed to setup server socket.");
        return false;
    }

    private void sendBannerMessage() {
        long pid = ProcessHandle.current().pid();
        System.out.println("================================================");
        System.out.println("Boot Listener started with PID: " + pid);
        System.out.println("Current date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
        System.out.println("Config path: " + config.getConfigPath());
        System.out.println("Boot Listener listening on port: " + port);
        System.out.println("================================================");
    }

    private void cleanup() {
        running = false;

        System.out.println("Shutting down server listener...");
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Server socket closed.");
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
        // stop client threads
        clientHandlers.shutdownNow();
    }

    private void startSocketServer() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(CLIENT_TIMEOUT);
                System.out.println("Connection received from: " + clientSocket.getInetAddress());
                clientHandlers.submit(() -> handleClient(clientSocket));
            } catch (SocketException e) {
                if (running) {
                    System.err.println("ServerSocket accept error: " + e.getMessage());
                } else {
                    System.out.println("Server shutting down gracefully.");
                }
                break;
            } catch (IOException e) {
                System.err.println("Error accepting client connection: " + e.getMessage());
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (InputStream input = clientSocket.getInputStream(); OutputStream output = clientSocket.getOutputStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(input)); PrintWriter writer = new PrintWriter(output, true)) {
            System.out.println("Client Thread waiting for command");
            // Read the client's message
            String command = reader.readLine();
            System.out.println("Received command: " + command);
            processRemoteCommand(command, writer);
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("Client socket closed successfully");
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void processRemoteCommand(String command, PrintWriter writer) {
        switch (command) {
            case "BOOT_SERVER":
                respond(writer, "ACKNOWLEDGED: Boot command received. Starting backend server...");
                if (startBackendServer()) {
                    respond(writer, "COMPLETED: Backend server started.");
                    running = false;
                } else {
                    respond(writer, "FAILED: Failed to start backend server.");
                }
                break;
            case "SHUTDOWN_BOOT_LISTENER":
                respond(writer, "SUCCESS: Shutting down boot listener.");
                running = false;
                break;
            default:
                respond(writer, "ERROR: Invalid command.");
        }
    }

    private void respond(PrintWriter writer, String message) {
        System.out.println("Sending > " + message);
        writer.println(message);
    }

    private void processCliCommand(String command) {
        switch (command) {
            case "help":
                System.out.println("Commands:");
                System.out.println("  help   - this message");
                System.out.println("  start  - start the backend server");
                System.out.println("  stop   - stop the boot listener");
                System.out.println("  reload - reload the config.yml file");
                break;
            case "reload":
                config.reload();
                break;
            case "stop":
                running = false;
                break;
            case "start":
                System.out.println("Starting backend server...");
                if (startBackendServer()) {
                    System.out.println("Command ran successfully.");
                    running = false;
                } else {
                    System.out.println("Failed to start backend server.");
                }
                break;
            default:
                System.out.println("Unknown command.");
        }
    }

    private boolean startBackendServer() {
        String command = config.getString("server.startCommand");
        if (command == null) {
            System.err.println("Error reading start command. Check config for required setting \"server.startCommand\".");
            return false;
        }

        String workingDirectory = config.getString("server.workingDirectory");
        Boolean preserveQuotes = config.getBoolean("server.preserveQuotes");

        try {
            CommandRunner.runCommand(workingDirectory, command, preserveQuotes);
        } catch (RuntimeException e) {
            return false;
        }

        return true;
    }
}