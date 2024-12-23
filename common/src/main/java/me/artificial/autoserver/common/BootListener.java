package me.artificial.autoserver.common;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BootListener {

    private final int port;
    private ServerSocket serverSocket;
    private final ExecutorService clientHandlers = Executors.newCachedThreadPool();
    private boolean running = true;

    public BootListener(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        long pid = ProcessHandle.current().pid();
        System.out.println("================================================");
        System.out.println("Boot Listener started with PID: " + pid);
        System.out.println("Current date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
        int port = Integer.parseInt(Config.getProperty("port"));

        new BootListener(port).start();

        // Sleep for 10 seconds after finishing for debug
        try {
            Thread.sleep(1000 * 10);
        } catch (InterruptedException e) {
            System.out.println("Interrupt received");
            Thread.currentThread().interrupt();
        }
    }

    private void start() {
        int retries = 10;
        int delay = 5000; // 5 seconds
        int timeout = 5000;
        while (running && retries-- > 0) {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("Boot Listener start on port: " + port);
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        clientSocket.setSoTimeout(timeout);
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
            } catch (IOException e) {
                System.out.println("Error starting server: " + e.getMessage());
            } finally {
                closeServerSocket();
            }

            if (running) {
                System.out.println("Retrying to start server in " + (delay / 1000) + " seconds...");
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    System.out.println("Retry delay interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt(); // Keep the interrupt going
                }
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (InputStream input = clientSocket.getInputStream(); OutputStream output = clientSocket.getOutputStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(input)); PrintWriter writer = new PrintWriter(output, true)) {
            System.out.println("Client Thread waiting for command");
            // Read the client's message
            String command = reader.readLine();
            System.out.println("Received command: " + command);
            processCommand(command, writer);
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

    private void processCommand(String command, PrintWriter writer) {
        switch (command) {
            case "BOOT_SERVER":
                respond(writer, "ACKNOWLEDGED: Boot command received. Starting backend server...");
                if (startBackendServer()) {
                    respond(writer, "COMPLETED: Backend server started.");
                    running = false;
                    closeServerSocket(); // Gracefully close the server socket
                } else {
                    respond(writer, "FAILED: Failed to start backend server.");
                }
                break;
            case "SHUTDOWN_BOOT_LISTENER":
                respond(writer, "SUCCESS: Shutting down boot listener.");
                running = false;
                closeServerSocket();
                break;
            default:
                respond(writer, "ERROR: Invalid command.");
        }
    }

    private void respond(PrintWriter writer, String message) {
        System.out.println("Sending > " + message);
        writer.println(message + '\n');
    }

    private boolean startBackendServer() {
        String command = Config.getProperty("startCommand");
        if (command == null) {
            System.err.println("Error reading start command.");
            return false;
        }
        return CommandRunner.runCommand(command);
    }

    private void closeServerSocket() {
        System.out.println("Shutting down Boot listener...");
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
}