package me.artificial.autoserver;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import org.slf4j.Logger;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ServerManager {

    private final HashMap<Player, String> queuePlayers = new HashMap<>();
    private final HashSet<String> startingServers = new HashSet<>();
    private final Logger logger;
    private final AutoServer plugin;

    public ServerManager(AutoServer autoServer) {
        this.plugin = autoServer;
        this.logger = autoServer.getLogger();
    }

    public void startServer(RegisteredServer server) {
        String serverName = server.getServerInfo().getName();
        logger.info("Starting server {}", serverName);
        if (startingServers.contains(serverName)) {
            logger.info("Server {} is already starting", serverName);
            return;
        }

        if (isRemote(server)) {
            logger.info("Attempting to start with remote command");
            sendCommandRemote(server, "start");
        } else {
            logger.info("Attempting to start with local command");
            try {
                String command = plugin.getStartCommand(serverName);
                logger.info("Running start command for {} server. '{}'", serverName, command);
                runCommand(command);
                pingUntilRunning(server);
            } catch (ConfigurationException e) {
                logger.error("Command not found with error {}", e.getMessage());
            }
        }
    }

    public void stopServer(RegisteredServer server) {
        String serverName = server.getServerInfo().getName();
        logger.info("Stopping server {}", serverName);
        if (startingServers.contains(serverName)) {
            logger.info("Server {} is in the boot process, wait until started before attempting to stop.", serverName);
            return;
        }

        if (isRemote(server)) {
            logger.info("Attempting to stop with remote command");
            sendCommandRemote(server, "shutdown");
        } else {
            logger.info("Attempting to stop with local command");
            try {
                String command = plugin.getStopCommand(serverName);
                logger.info("Running stop command for {} server. '{}'", serverName, command);
                runCommand(command);
            } catch (ConfigurationException e) {
                logger.error("Command not found with error {}", e.getMessage());
            }
        }
    }

    private boolean isRemote(RegisteredServer server) {
        // TODO Write method
        return true;
    }

    private void sendCommandRemote(RegisteredServer server, String command) {
        // Attempt to open a client connection to the backend server
        InetAddress ip = server.getServerInfo().getAddress().getAddress();
        CommandSocketClient client = new CommandSocketClient(logger);
        if (client.startConnection(ip)) {
            logger.info("Sending command `{}` to {}", command, ip);
            String resp = client.sendMessage(command);
            logger.info("Server response {}", resp);
            client.stopConnection();
        }
    }

    public void delayedPlayerJoin(Player player, String serverName) {
        queuePlayers.put(player, serverName);
    }

    private void movePlayers(RegisteredServer server) {
        queuePlayers.forEach((player, serverName) -> {
            if (serverName.equals(server.getServerInfo().getName())) {
                if (player.isActive()) {
                    // Notify the player
                    AutoServer.sendMessageToPlayer(player, plugin.getNotifyMessage());

                    // Schedule the connection request to run after 5 seconds
                    plugin.getProxy().getScheduler().buildTask(plugin, () -> {
                        player.createConnectionRequest(server).connect().whenComplete((complete, throwable) -> {
                            if (throwable != null) {
                                AutoServer.sendMessageToPlayer(player, plugin.getFailedMessage(), serverName);
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
        new Thread(() -> {
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
            // Failed to start server
            startingServers.remove(serverName);
            logger.error("Failed to launch server {}", serverName);
            queuePlayers.forEach((player, sn) -> {
                if (serverName.equals(sn)) {
                    AutoServer.sendMessageToPlayer(player, plugin.getFailedMessage(), serverName);
                }
            });
        }).start();
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