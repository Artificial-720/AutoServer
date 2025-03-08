package me.artificial.autoserver.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import me.artificial.autoserver.velocity.startable.LocalStartable;
import me.artificial.autoserver.velocity.startable.RemoteStartable;
import me.artificial.autoserver.velocity.startable.Startable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * A ServerManager class that manages the state of servers, including starting, stopping,
 * and checking the status of servers.
 */
public class ServerManager {
    private final AutoServerLogger logger;
    private final AutoServer plugin;
    private final HashSet<String> startingServers = new HashSet<>();
    private final HashMap<Player, String> queuePlayers = new HashMap<>();
    private final Map<String, ServerStatus> serverStatusCache = new ConcurrentHashMap<>();
    private final Map<String, ScheduledTask> shutdownScheduledTask = new ConcurrentHashMap<>();

    public ServerManager(AutoServer plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Starts a given server if it is not already running.
     *
     * @param server The server to start.
     * @return A CompletableFuture that completes with a success message if the server starts successfully,
     *         or completes exceptionally if an error occurs or the server is already running.
     */
    public CompletableFuture<String> startServer(RegisteredServer server) {
        // Check if already starting
        String serverName = server.getServerInfo().getName();
        if (startingServers.contains(serverName)) {
            logger.debug("Server {} is already starting", serverName);
            return CompletableFuture.completedFuture("Server is already starting.");
        }

        startingServers.add(serverName);

        logger.info("Attempting to start server: {}", serverName);

        // Determine start strategy
        Startable startableStrategy = getServerStrategy(server);

        // Do a long ping to check if server is actually offline before trying to start it
        return isServerOnline(server)
                .thenCompose(isOnline -> {
                    if (isOnline) {
                        // Already running
                        moveQueuedPlayersToServer(server);
                        return CompletableFuture.completedFuture("Server already running");
                    }

                    // Finally start the server using the given strategy
                    return startableStrategy.start()
                            .thenCompose(result -> waitForServerToBecomeResponsive(server)
                                    .thenApply(isResponsive -> {
                                        if (isResponsive) {
                                            // Return the result after server becomes responsive.
                                            moveQueuedPlayersToServer(server);
                                            return "Server started and is responsive.";
                                        } else {
                                            // Return an error message if the server is not responsive.
                                            throw new RuntimeException("Server started but is not responsive.");
                                        }
                                    }));
                })
                .whenComplete((result, ex) -> {
                    // clean up
                    startingServers.remove(serverName);
                    if (ex != null) {
                        logger.error("Failed to start server: {}", ex.getMessage());
                    }
                });
    }

    /**
     * Stops a given server if it is currently running.
     *
     * @param server The server to stop.
     * @return A CompletableFuture that completes with a success message if the server stops successfully,
     *         or completes exceptionally if an error occurs or the server is already stopped.
     */
    public CompletableFuture<String> stopServer(RegisteredServer server) {
        // Check if already stopping
        String serverName = server.getServerInfo().getName();
        if (getServerStatus(server).isStopping()) {
            logger.debug("Server {} is already stopping", serverName);
            return CompletableFuture.completedFuture("Server is already stopping.");
        }

        getServerStatus(server).setStatus(ServerStatus.Status.STOPPING);

        logger.info("Attempting to stop server: {}", serverName);
        Startable startableStrategy = getServerStrategy(server);

        // Do a long ping to check if server is actually online before trying to stop it
        return isServerOnline(server)
                .thenCompose(isOnline -> {
                    if (!isOnline) {
                        return CompletableFuture.completedFuture("Server already stopped");
                    }

                    // Finally stop the server using the given strategy
                    return startableStrategy.stop()
                            .thenCompose(result -> {
                                long shutdownDelay = plugin.getConfig().getShutdownDelay(server);

                                // Delay a little bit before trying to ping to give server time to stop
                                try {
                                    logger.info("Sleeping for {} seconds before checking if server has stopped.", shutdownDelay);
                                    Thread.sleep(shutdownDelay * 1000);
                                } catch (InterruptedException e) {
                                    logger.warn("Stop delay sleep interrupted: {}", e.getMessage());
                                    Thread.currentThread().interrupt();
                                }


                                return isServerOnline(server).thenApply(isOnline2 -> {
                                        if (isOnline2) {
                                            throw new RuntimeException("Failed to stop server.");
                                        } else {
                                            return "Server stopped.";
                                        }
                                    });
                            });
                })
                .whenComplete((result, ex) -> {
                    // clean up
                    if (ex != null) {
                        logger.error("Failed to stop server: {}", ex.getMessage());
                        getServerStatus(server).setStatus(ServerStatus.Status.UNKNOWN);
                    } else {
                        getServerStatus(server).setStatus(ServerStatus.Status.STOPPED);
                    }
                });
    }

    /**
     * Checks if the specified server is online (i.e., fully operational and ready to accept connections).
     *
     * @param server The server to check.
     * @return A CompletableFuture that completes with true if the server is online, false otherwise.
     */
    public CompletableFuture<Boolean> isServerOnline(RegisteredServer server) {
        return pingServer(server, 5000);
    }

    /**
     * Performs a quick check on server responsiveness, including checking cache and optionally pinging the server.
     * The state of the server may not be guaranteed with this check.
     *
     * @param server The server to check.
     * @return A CompletableFuture that completes with true if the server is responsive, false otherwise.
     */
    public CompletableFuture<Boolean> isServerResponsive(RegisteredServer server) {
        String serverName = server.getServerInfo().getName();
        ServerStatus cachedStatus = getServerStatus(server);

        if (cachedStatus.is(ServerStatus.Status.STOPPED)) {
            logger.debug("Cache check for server '{}' is OFFLINE", serverName);
            return CompletableFuture.completedFuture(false);
        }
        if (!server.getPlayersConnected().isEmpty()) {
            logger.debug("Players detected on server '{}', assuming ONLINE", serverName);
            return CompletableFuture.completedFuture(true);
        }

        // cache not valid need to ping
        return pingServer(server, 50);
    }

    /**
     * Retrieves the current status of the specified server.
     *
     * @param server The server whose status is to be retrieved.
     * @return The current status of the server (e.g., ONLINE, OFFLINE, UNKNOWN).
     */
    public ServerStatus getServerStatus(RegisteredServer server) {
        String serverName = server.getServerInfo().getName();
        if (!serverStatusCache.containsKey(serverName)) {
            serverStatusCache.put(serverName, new ServerStatus());
            try {
                isServerOnline(server).get();
            } catch (InterruptedException | ExecutionException ignored) {
            }
        }

        return serverStatusCache.get(serverName);
    }

    /**
     * Queues a player to join a server once it's started and available.
     *
     * @param player The player to queue.
     * @param serverName The name of the server the player is waiting to join.
     */
    public void queuePlayerForServerJoin(Player player, String serverName) {
        queuePlayers.put(player, serverName);
    }

    /**
     * Schedules server for shutdown.
     *
     * @param server The server to be scheduled for shutdown.
     */
    public void scheduleShutdownServer(RegisteredServer server, boolean isDisconnecting) {
        assert server != null;
        logger.trace("scheduleShutdownServer: {}", server.getServerInfo().getName());
        int playerCount = server.getPlayersConnected().size();
        if (isDisconnecting) {
            playerCount--;
        }
        if (playerCount <= 0) {
            long autoShutdownDelay = plugin.getConfig().getAutoShutdownDelay(server);
            if (autoShutdownDelay <= 0) {
                return;
            }

            assert !shutdownScheduledTask.containsKey(server.getServerInfo().getName()) : "Server already has task scheduled for shutdown.";

            logger.info("Scheduling shutdown of server {} in {}", server.getServerInfo().getName(), autoShutdownDelay);
            Scheduler.TaskBuilder taskBuilder = plugin.getProxy().getScheduler()
                    .buildTask(plugin, () -> stopServer(server).whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.error("error: {}", ex.getMessage());
                        } else {
                            logger.info("Message: {}", result);
                        }
                    })).delay(Duration.ofSeconds(autoShutdownDelay));

            ScheduledTask scheduledTask = taskBuilder.schedule();
            shutdownScheduledTask.put(server.getServerInfo().getName(), scheduledTask);
        }
    }

    public void cancelShutdownServer(RegisteredServer server) {
        String serverName = server.getServerInfo().getName();
        if (shutdownScheduledTask.containsKey(serverName)) {
            logger.info("Cancelling auto shutdown: {}", serverName);
            shutdownScheduledTask.get(serverName).cancel();
            shutdownScheduledTask.remove(serverName);
        }
    }

    public void validateServers(Collection<RegisteredServer> servers) {
        logger.trace("Validating Server status...");
        for (RegisteredServer server : servers) {
            pingServer(server, 5000).thenApply((isOnline) -> {
                if (isOnline) {
                    scheduleShutdownServer(server, false);
                }
                return null;
            });
        }
    }

    private Startable getServerStrategy(RegisteredServer server) {
        Optional<Boolean> remote = plugin.getConfig().isRemoteServer(server);
        if (remote.isPresent() && remote.get()) {
            return new RemoteStartable(plugin, server);
        }
        return new LocalStartable(plugin, server);
    }

    private CompletableFuture<Boolean> pingServer(RegisteredServer server, int pingTimeout) {
        String serverName = server.getServerInfo().getName();
        logger.debug("Pinging server {}...", serverName);
        return server.ping().orTimeout(pingTimeout, TimeUnit.MILLISECONDS).thenApply(serverPing -> {
            logger.debug("ping success {} is {}online{}", serverName, AnsiColors.GREEN, AnsiColors.RESET);
            if (!getServerStatus(server).isStopping()) {
                // only update to running if not in a state of stopping
                getServerStatus(server).setStatus(ServerStatus.Status.RUNNING);
            }
            return true;
        }).exceptionally(e -> {
            logger.debug("ping failed {} is {}offline{}", serverName, AnsiColors.RED, AnsiColors.RESET);
            if (!getServerStatus(server).isStarting()) {
                // only update to stopped if not in a state of starting
                getServerStatus(server).setStatus(ServerStatus.Status.STOPPED);
            }
            return false;
        });
    }

    /**
     * Continuously pings the specified server until it becomes responsive and ready.
     *
     * @param server The server to ping.
     * @return A CompletableFuture that completes with true when the server is ready, false if an error occurs or the server does not respond.
     */
    private CompletableFuture<Boolean> waitForServerToBecomeResponsive(RegisteredServer server) {
        return CompletableFuture.supplyAsync(() ->{
            int retires = 10;
            int delayBetweenRetries = 5; // seconds
            long startupDelay = plugin.getConfig().getStartUpDelay(server);

            // Delay a little bit before trying to ping to give server time to start
            try {
                logger.info("Sleeping for {} seconds before checking if server has started.", startupDelay);
                Thread.sleep(startupDelay * 1000);
            } catch (InterruptedException e) {
                logger.warn("Ping delay sleep interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }

            // ping server with retires
            while (retires > 0) {
                try {
                    if (pingServer(server, 5000).get()) {
                        logger.info("Server {} is {}online{}. Moving queued players...", server.getServerInfo().getName(), AnsiColors.GREEN, AnsiColors.RESET);
                        return true;
                    } else {
                        logger.debug("Failed to ping server {}. Retrying in {} seconds.", server.getServerInfo().getName(), delayBetweenRetries);
                    }
                } catch (ExecutionException | InterruptedException e) {
                    logger.debug("Failed to ping server {}: {}. Retrying in {} seconds.", server.getServerInfo().getName(), e.getMessage(), delayBetweenRetries);
                }

                retires--;
                if (retires > 0) {
                    try {
                        Thread.sleep(delayBetweenRetries * 1000L);
                    } catch (InterruptedException e) {
                        logger.warn("Ping retry sleep interrupted: {}", e.getMessage());
                        Thread.currentThread().interrupt();
                    }
                }
            }

            return false;
        });
    }

    /**
     * Moves all players from the queue to the specified server once the server has started and is ready.
     *
     * @param server The server to which queued players will be moved.
     */
    private void moveQueuedPlayersToServer(RegisteredServer server) {
        queuePlayers.forEach((player, serverName) -> {
            if (serverName.equals(server.getServerInfo().getName())) {
                if (player.isActive()) {
                    // Notify the player
                    if (player.getCurrentServer().isPresent()) {
                        AutoServer.sendMessageToPlayer(player, plugin.getConfig().getMessage("notify").orElse(""));
                        // Schedule the connection request to run after 5 seconds
                        plugin.getProxy().getScheduler().buildTask(plugin, () ->
                                player.createConnectionRequest(server).connect().whenComplete((result, throwable) -> {
                                    if (throwable != null) {
                                        AutoServer.sendMessageToPlayer(player, plugin.getConfig().getMessage("failed").orElse(""), serverName);
                                        logger.error("Failed to connect player to server {}", throwable.getMessage());
                                    } else {
                                        logger.info("Player {} successfully moved to server {}", player.getUsername(), serverName);
                                    }
                                    queuePlayers.remove(player);
                                })).delay(5, TimeUnit.SECONDS).schedule();
                    } else {
                        // Not connected to a server so want to connect fast
                        player.createConnectionRequest(server).connect().whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                AutoServer.sendMessageToPlayer(player, plugin.getConfig().getMessage("failed").orElse(""), serverName);
                                logger.error("Failed to connect player to server {}", throwable.getMessage());
                            } else {
                                logger.info("Player {} successfully moved to server {}", player.getUsername(), serverName);
                            }
                            queuePlayers.remove(player);
                        });
                    }

                }
            }
        });
    }
}
