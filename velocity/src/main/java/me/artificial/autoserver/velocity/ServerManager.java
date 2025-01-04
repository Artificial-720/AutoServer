package me.artificial.autoserver.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import me.artificial.autoserver.common.CommandRunner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ServerManager {
    public static final int TIMEOUT = 5000;
    private final static String COMMAND_BOOT = "BOOT_SERVER\n";
    private final static int REMOTE_PORT = 8080;
    private final HashMap<Player, String> queuePlayers = new HashMap<>();
    private final HashSet<String> startingServers = new HashSet<>();
    private final Logger logger;
    private final AutoServer autoServer;
    private final Map<String, ServerStatusCache> serverStatusCache = new ConcurrentHashMap<>();

    public ServerManager(AutoServer autoServer) {
        this.autoServer = autoServer;
        this.logger = autoServer.getLogger();
    }

    public CompletableFuture<Boolean> isServerOnline(RegisteredServer server) {
        String serverName = server.getServerInfo().getName();
        ServerStatusCache cachedStatus = serverStatusCache.get(serverName);

        if (cachedStatus != null && !cachedStatus.isOnline) {
            logger.info("Cache check for server '{}' is OFFLINE", serverName);
            return CompletableFuture.completedFuture(false);
        }
        if (!server.getPlayersConnected().isEmpty()) {
            logger.info("Players detected on server '{}', assuming ONLINE", serverName);
            return CompletableFuture.completedFuture(true);
        }

        // cache not valid need to ping
        return pingServer(server, 50);
    }

    public void refreshServerCache(Collection<RegisteredServer> servers) {
        logger.info("Refreshing Server cache...");
        for (RegisteredServer server : servers) {
            pingServer(server, 5000);
        }
    }

    public void delayedPlayerJoin(Player player, String serverName) {
        queuePlayers.put(player, serverName);
    }

    public void startServer(RegisteredServer server) {
        String serverName = server.getServerInfo().getName();
        if (startingServers.contains(serverName)) {
            logger.info("Server {} is already starting", serverName);
            return;
        }

        startingServers.add(serverName);

        new Thread(() -> {
            logger.info("Starting server {}", serverName);

            // Do a long ping to check if server is actually offline before trying to start it
            CompletableFuture<Boolean> isOnline = pingServer(server, 5000);

            try {
                if (isOnline.get()) {
                    // Already running
                    startingServers.remove(serverName);
                    movePlayers(server);
                    return;
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Failed to determine server status, assuming offline");
            }

            Optional<Boolean> remote = autoServer.isRemoteServer(server);

            if (remote.isPresent() && remote.get()) {
                logger.info("Attempting to start with remote command");
                sendCommandRemote(server);
            } else {
                logger.info("Attempting to start with local command");
                sendCommandLocal(server);
            }
        }).start();
    }

    private CompletableFuture<Boolean> pingServer(RegisteredServer server, int pingTimeout) {
        String serverName = server.getServerInfo().getName();
        logger.info("Pinging server {}...", serverName);
        return server.ping().orTimeout(pingTimeout, TimeUnit.MILLISECONDS).thenApply(serverPing -> {
            logger.info("ping success {} is online", serverName);
            serverStatusCache.put(serverName, new ServerStatusCache(true));
            return true;
        }).exceptionally(e -> {
            logger.info("ping failed {} is offline", serverName);
            serverStatusCache.put(serverName, new ServerStatusCache(false));
            return false;
        });
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
            case "ACKNOWLEDGED": // Backend server received the boot command and has acknowledged it
                logger.info("Backend server has acknowledged boot command.");
                break;
            case "COMPLETED": // Backend server has executed the boot command successfully but is not yet running
                logger.info("Backend server booting.");
                pingUntilRunning(server);
                break;
            case "READY": // Backend server is fully booted and running
                break;
            case "FAILED": // Backend server encountered an error during boot
                logger.error("Backend server failed to start. Message: {}", message);
                failedToStartBackend(server.getServerInfo().getName());
                break;
            case "ERROR": // Backend server encountered an error
                logger.warn("Error occurred on the backend server with message: {}", message);
                break;
            default: // Unrecognized status received from the backend
                logger.warn("Unexpected status received: {}. Message: {}", status, message);
                break;
        }
    }

    private void movePlayers(RegisteredServer server) {
        queuePlayers.forEach((player, serverName) -> {
            if (serverName.equals(server.getServerInfo().getName())) {
                if (player.isActive()) {
                    // Notify the player
                    AutoServer.sendMessageToPlayer(player, autoServer.getMessage("notify").orElse(""));

                    // Schedule the connection request to run after 5 seconds
                    autoServer.getProxy().getScheduler().buildTask(autoServer, () ->
                        player.createConnectionRequest(server).connect().whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                AutoServer.sendMessageToPlayer(player, autoServer.getMessage("failed").orElse(""), serverName);
                                logger.error("Failed to connect player to server {}", throwable.getMessage());
                            } else {
                                logger.info("Player {} successfully moved to server {}", player.getUsername(), serverName);
                            }
                            queuePlayers.remove(player);
                        })).delay(5, TimeUnit.SECONDS).schedule();
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

                    startingServers.remove(serverName);
                    serverStatusCache.put(serverName, new ServerStatusCache(true));
                    movePlayers(server);

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

    private void sendCommandLocal(RegisteredServer server) {
        Optional<String> command = autoServer.getStartCommand(server.getServerInfo().getName());
        if (command.isEmpty()) {
            logger.error("Command not found.");
            failedToStartBackend(server.getServerInfo().getName());
            return;
        }

        logger.info("Running start command for {} server. '{}'", server.getServerInfo().getName(), command);
        if (CommandRunner.runCommand(command.get())) {
            pingUntilRunning(server);
        } else {
            logger.error("Command Failed.");
            failedToStartBackend(server.getServerInfo().getName());
        }
    }

    private void sendCommandRemote(RegisteredServer server) {
        InetAddress ip = server.getServerInfo().getAddress().getAddress();
        Optional<Integer> port = autoServer.getPort(server);
        try (Socket socket = new Socket(ip, port.orElse(REMOTE_PORT))) {
            socket.setSoTimeout(TIMEOUT);
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

    private void failedToStartBackend(String serverName) {
        // Failed to start server
        startingServers.remove(serverName);
        logger.error("Failed to launch server {}", serverName);

        // message each player and remove from queue
        List<Player> playersToRemove = new ArrayList<>();
        queuePlayers.forEach((player, sn) -> {
            if (sn.equals(serverName)) {
                AutoServer.sendMessageToPlayer(player, autoServer.getMessage("failed").orElse(""), serverName);

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

}
