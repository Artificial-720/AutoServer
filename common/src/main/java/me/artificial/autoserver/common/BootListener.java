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
    private final static String PROMPT = "> ";
    private final static int CLIENT_TIMEOUT = 5000;
    private final static int SERVER_RETRIES = 10;
    private final static int DELAY_BETWEEN_RETIRES = 5000; // 5 seconds

    private final ExecutorService clientHandlers = Executors.newCachedThreadPool();
    private Integer port = null;
    private BackendConfig config = null;
    private ServerSocket serverSocket = null;
    private volatile boolean running = true;
    private Thread socketThread;
    private Thread cliThread;

    public static void main(String[] args) {
        BootListener bootListener = new BootListener();
        bootListener.start();
    }

    private void start() {
        if (!initialize()) return;

        if (!setupServerSocket()) return;

        sendBannerMessage();

        socketThread = new Thread(this::socketServerLoop);
        cliThread = new Thread(this::cliLoop);

        socketThread.start();
        cliThread.start();

        // Wait for both treads to finish
        try {
            cliThread.join();
            socketThread.join();
        } catch (InterruptedException ignored) {}

        System.out.println("Backend Listener exited.");
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
        System.out.println("Type \"help\" for list of cli commands.");
        System.out.println("================================================");
    }

    private void cliLoop() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print(PROMPT);
        while (running) {
            try {
                if (reader.ready()) {
                    String command = reader.readLine();
                    processCliCommand(command);
                    if (running) {
                        System.out.print(PROMPT);
                    }
                }
                Thread.sleep(100); // Avoid hogging CPU
            } catch (Exception ignored) {}
        }
    }

    private void socketServerLoop() {
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
        Boolean enabled = config.getBoolean("security.enabled", true);
        String secret = config.getString("security.secret");
        if (enabled != null && enabled && secret == null) {
            System.out.println("Security is enabled, but the required setting \"security.secret\" is missing. Please add the setting and restart.");
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
            System.out.println("Client socket closed successfully");
            return;
        }

        try (InputStream input = clientSocket.getInputStream();
             OutputStream output = clientSocket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input));
             PrintWriter writer = new PrintWriter(output, true)) {
            System.out.println("Client Thread waiting for command");
            // Read the client's message
            String command = reader.readLine();

            if (enabled != null && enabled) {
                System.out.println("Client Thread waiting for signature");
                String receivedSignature = reader.readLine();

                System.out.println("Command: " + command);
                System.out.println("Signature: " + receivedSignature);

                // verify
                boolean isValid = HMAC.verifyMessage(command, receivedSignature, secret);
                if (isValid) {
                    System.out.println("Authenticated command: " + command);
                    processRemoteCommand(command, writer);
                } else {
                    System.out.println("Authentication failed! Rejecting command.");
                }
            } else {
                System.out.println("Received command: " + command);
                processRemoteCommand(command, writer);
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                Thread.sleep(50);  // Short delay before closing
                clientSocket.close();
                System.out.println("Client socket closed successfully");
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("InterruptedException: " + e.getMessage());
                Thread.currentThread().interrupt(); // Keep the interrupt going
            }
        }
    }

    private void processRemoteCommand(String command, PrintWriter writer) {
        switch (command) {
            case NetworkCommands.BOOT:
                respond(writer, NetworkCommands.buildMessage(NetworkCommands.ACKNOWLEDGED, "Boot command received. Running start command..."));
                if (startBackendServer()) {
                    respond(writer, NetworkCommands.buildMessage(NetworkCommands.COMPLETED, "Backend server starting."));
                    stopAll();
                } else {
                    respond(writer, NetworkCommands.buildMessage(NetworkCommands.FAILED, "Failed to start backend server."));
                }
                break;
            case NetworkCommands.SHUTDOWN_BOOT_LISTENER:
                respond(writer, NetworkCommands.buildMessage(NetworkCommands.SUCCESS, "Shutting down boot listener."));
                stopAll();
                break;
            default:
                respond(writer, NetworkCommands.buildMessage(NetworkCommands.ERROR, "Invalid command."));
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
                System.out.println("  reload - reload the config.yml file (port is not hot reloadable)");
                break;
            case "reload":
                config.reload();
                break;
            case "stop":
                stopAll();
                break;
            case "start":
                System.out.println("Starting backend server...");
                if (startBackendServer()) {
                    System.out.println("Command ran successfully.");
                    stopAll();
                } else {
                    System.out.println("Failed to start backend server.");
                }
                break;
            default:
                System.out.println("Unknown command.");
        }
    }

    private void stopAll() {
        // doing this on a thread so that we don't
        // interrupt this function from finishing
        new Thread(() -> {
            running = false;

            // Close server socket
            System.out.println("Shutting down server listener...");
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                    System.out.println("Server socket closed.");
                }
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
            System.out.println("Stopping client threads");
            // stop client threads
            clientHandlers.shutdownNow();

            cliThread.interrupt(); // Interrupt CLI thread (if blocked)
            socketThread.interrupt(); // Interrupt Socket thread (if blocked)
        }).start();
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
        } catch (RuntimeException ignored) {
            return false;
        }

        return true;
    }
}