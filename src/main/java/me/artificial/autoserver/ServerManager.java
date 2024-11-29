package me.artificial.autoserver;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import org.slf4j.Logger;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ServerManager {

    private final HashMap<Player, String> queuePlayers = new HashMap<>();
    private final HashSet<String> startingServers = new HashSet<>();
    private final Logger logger;
    private final AutoServer autoServer;

    public ServerManager(AutoServer autoServer) {
        this.autoServer = autoServer;
        this.logger = autoServer.getLogger();
    }

    public void startServer(RegisteredServer server) {
        String serverName = server.getServerInfo().getName();
        logger.info("Starting server {}", serverName);
        if (startingServers.contains(serverName)) {
            logger.info("Server {} is already starting", serverName);
            return;
        }
        try {
            String command = autoServer.getStartCommand(serverName);
            logger.info("Running start command for {} server. '{}'", serverName, command);
            runCommand(command);
            pingUntilRunning(server);
        } catch (ConfigurationException e) {
            logger.error("Command not found with error {}", e.getMessage());
        }
    }

    public void delayedPlayerJoin(Player player, String serverName) {
        queuePlayers.put(player, serverName);
    }

    public HashSet<String> getStartingServers() {
        return startingServers;
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
        // Failed to start server
        startingServers.remove(serverName);
        logger.error("Failed to launch server {}", serverName);
        queuePlayers.forEach((player, sn) -> {
            if (serverName.equals(sn)) {
                AutoServer.sendMessageToPlayer(player, autoServer.getFailedMessage(), serverName);
            }
        });
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
