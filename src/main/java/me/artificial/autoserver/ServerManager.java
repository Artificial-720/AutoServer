package me.artificial.autoserver;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ServerManager {
    private final static String COMMAND_BOOT = "BOOT_SERVER";
    private final static int REMOTE_PORT = 8080;

    private final HashMap<Player, String> queuePlayers = new HashMap<>();
    private final HashSet<String> startingServers = new HashSet<>();
    private final Logger logger;
    private final AutoServer autoServer;

    public ServerManager(AutoServer autoServer) {
        this.autoServer = autoServer;
        this.logger = autoServer.getLogger();
    }

    public void startServer(RegisteredServer server) {
        new Thread(() -> {
            String serverName = server.getServerInfo().getName();
            if (startingServers.contains(serverName)) {
                logger.info("Server {} is already starting", serverName);
                return;
            }

            logger.info("Starting server {}", serverName);

            if (autoServer.isRemoteServer(server)) {
                logger.info("Attempting to start with remote command");
                sendCommandRemote(server);
            } else {
                logger.info("Attempting to start with local command");
                sendCommandLocal(server);
            }
        });
    }

    public void delayedPlayerJoin(Player player, String serverName) {
        queuePlayers.put(player, serverName);
    }

    private void sendCommandLocal(RegisteredServer server) {
        String serverName = server.getServerInfo().getName();
        try {
            String command = autoServer.getStartCommand(serverName);
            logger.info("Running start command for {} server. '{}'", serverName, command);
            runCommand(command);
            pingUntilRunning(server);
        } catch (ConfigurationException e) {
            logger.error("Command not found with error {}", e.getMessage());
        }
    }

    private void sendCommandRemote(RegisteredServer server) {
        InetAddress ip = server.getServerInfo().getAddress().getAddress();
        try (Socket socket = new Socket(ip, REMOTE_PORT)) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            logger.info("Attempting to send BOOT_SERVER command");
            out.write(COMMAND_BOOT.getBytes());
            out.flush();

            byte[] buffer = new byte[1024];
            int read;
            boolean noResponse = true;

            while ((read = in.read(buffer)) != -1) {
                String response = new String(buffer, 0, read);
                noResponse = false;
                logger.info("Server Response: {}", response.trim());
                handleResponse(server, response.trim());
            }
            if (noResponse) {
                logger.info("No response received from the server.");
                failedToStartBackend(server.getServerInfo().getName());
            }
        } catch (Exception e) {
            logger.warn("Error while communicating with the server: {}", e.getMessage());
            failedToStartBackend(server.getServerInfo().getName());
        }
    }

    private void handleResponse(RegisteredServer server, String response) {
        if (response == null || response.isBlank()) {
            logger.warn("Received an empty or null response.");
            return;
        }

        String[] parts = response.split(":", 2);
        if (parts.length < 2) {
            logger.warn("Malformed response received: {}", response);
            return;
        }

        String status = parts[0].trim();
        String message = parts[1].trim();
        logger.info("Response Status: {}, Message: {}", status, message);

        switch (status.toUpperCase()) {
            case "SUCCESS":
                pingUntilRunning(server);
                break;
            case "ERROR":
                logger.error("Backend server failed to start. Message: {}", message);
                failedToStartBackend(server.getServerInfo().getName());
                break;
            default:
                logger.warn("Unexpected status received: {}. Message: {}", status, message);
                break;
        }
    }

    private void movePlayers(RegisteredServer server) {
        queuePlayers.forEach((player, serverName) -> {
            if (serverName.equals(server.getServerInfo().getName())) {
                if (player.isActive()) {
                    // Notify the player
                    AutoServer.sendMessageToPlayer(player, autoServer.getNotifyMessage());

                    // Schedule the connection request to run after 5 seconds
                    autoServer.getProxy().getScheduler().buildTask(autoServer, () -> {
                        player.createConnectionRequest(server).connect().whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                AutoServer.sendMessageToPlayer(player, autoServer.getFailedMessage(), serverName);
                                logger.error("Failed to connect player to server {}", throwable.getMessage());
                            } else {
                                queuePlayers.remove(player);
                                logger.info("Player {} successfully moved to server {}", player.getUsername(), serverName);
                            }
                        });
                    }).delay(5, TimeUnit.SECONDS).schedule();
                }
            }
        });
    }

    private void pingUntilRunning(RegisteredServer server) {
        int retries = 10;
        int delayBetweenRetries = 5;
        String serverName = server.getServerInfo().getName();

        while (retries > 0) {
            try {
                ServerPing serverPing = server.ping().get();
                if (serverPing != null) {
                    logger.info("Server {} is online. Moving queued players...", serverName);

                    movePlayers(server);
                    startingServers.remove(serverName);

                    return;
                }
            } catch (ExecutionException | InterruptedException e) {
                logger.warn("Failed to ping server {}: {}. Retrying in {} seconds.", serverName, e.getMessage(), delayBetweenRetries);
            }

            retries--;
            if (retries > 0) {
                try {
                    Thread.sleep(delayBetweenRetries * 1000); // Wait before retrying
                } catch (InterruptedException e) {
                    logger.warn("Ping retry sleep interrupted: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        }
        failedToStartBackend(serverName);
    }

    private void failedToStartBackend(String serverName) {
        // Failed to start server
        startingServers.remove(serverName);
        logger.error("Failed to launch server {}", serverName);

        // message each player and remove from queue
        List<Player> playersToRemove = new ArrayList<>();
        queuePlayers.forEach((player, sn) -> {
            if (sn.equals(serverName)) {
                AutoServer.sendMessageToPlayer(player, autoServer.getFailedMessage(), serverName);

                // Check if connected to a server already
                Optional<ServerConnection> playerCurrentServer = player.getCurrentServer();
                if (playerCurrentServer.isEmpty()) {
                    player.disconnect(Component.text("Failed to start server " + serverName).color(NamedTextColor.RED));
                }

                playersToRemove.add(player);
            }
        });
        playersToRemove.forEach(queuePlayers::remove);
    }

    private void runCommand(String command) {
        // Determine the operating system
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder processBuilder;

        // Set the command based on the OS
        if (os.contains("win")) {
            // Windows command
            logger.info("Windows Detected");
            processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            // Linux/Unix command
            logger.info("Linux/Unix Detected");
            processBuilder = new ProcessBuilder("bash", "-c", command);
        }

        // Start the process
        try {
            processBuilder.start();
        } catch (IOException e) {
            logger.error("Execution while running command: {}", e.toString());
        }
    }
}
