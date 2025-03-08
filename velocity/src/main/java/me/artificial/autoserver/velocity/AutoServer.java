package me.artificial.autoserver.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.artificial.autoserver.velocity.commands.AutoServerCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Plugin(id = "autoserver")
public class AutoServer {
    private final ProxyServer proxy;
    private final AutoServerLogger logger;
    private final PluginContainer pluginContainer;
    private final Configuration config;
    private ServerManager serverManager;

    @SuppressWarnings("unused")
    @Inject
    public AutoServer(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory, PluginContainer pluginContainer) {
        // DON'T ACCESS VELOCITY API HERE
        this.proxy = proxy;
        this.config = new Configuration(dataDirectory);
        this.logger = new AutoServerLogger(this, logger);
        this.pluginContainer = pluginContainer;
    }

    public static void sendMessageToPlayer(Player player, String message) {
        sendMessageToPlayer(player, message, null);
    }

    public static void sendMessageToPlayer(Player player, String message, String serverName) {
        if (message == null) {
            return;
        }
        if (serverName != null) {
            message = message.replace("%serverName%", serverName);
        }
        MiniMessage miniMessage = MiniMessage.miniMessage();
        Component component = miniMessage.deserialize(message);
        player.sendMessage(component);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // velocity starting up (register event listeners here)
        logger.info("Loading configuration...");
        try {
            config.reloadConfig();
        } catch (Exception e) {
            logger.error("Failed to load config! Stopping plugin initialization.");
            logger.error("");
            logger.error(e.getMessage());
            logger.error("");
            throw new RuntimeException("Failed to load config! Stopping plugin initialization.");
        }
        logger.info("Configuration Loaded");

        serverManager = new ServerManager(this);

        CommandManager commandManager = proxy.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("autoserver").aliases("as").plugin(this).build();
        commandManager.register(commandMeta, new AutoServerCommand(this));

        if (config.checkForUpdate()) {
            notifyUpdates();
        }

//        serverManager.refreshServerCache(proxy.getAllServers());
        serverManager.validateServers(proxy.getAllServers());
        logger.info("Successfully enabled AutoServer");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        // TODO Maybe shutdown all servers
        logger.info("Successfully disabled AutoServer");
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        long startTime = System.nanoTime();
        // Check if the target server should be started
        RegisteredServer originalServer = event.getOriginalServer(); // Server trying to connect too
        RegisteredServer previousServer = event.getPreviousServer(); // Server was connected too
        String originalServerName = originalServer.getServerInfo().getName();
        logger.debug("Player {} attempting to join {}", event.getPlayer().getUsername(), originalServerName);

        // cancel schedule shutdown for server
        serverManager.cancelShutdownServer(originalServer);

        CompletableFuture<Boolean> isResponsive = serverManager.isServerResponsive(originalServer);
        try {
            if (isResponsive.get()) {
                logger.info("Server {}{}{} is online allowing connection", AnsiColors.GREEN, originalServerName, AnsiColors.RESET);
                event.setResult(ServerPreConnectEvent.ServerResult.allowed(originalServer));
            } else {
                // server not online need to start it
                logger.info("Server {}{}{} is not online attempting to start server", AnsiColors.RED, originalServerName, AnsiColors.RESET);
                event.setResult(ServerPreConnectEvent.ServerResult.denied());

                if (previousServer != null) {
                    sendMessageToPlayer(event.getPlayer(), config.getMessage("starting").orElse(""), originalServerName);
                }
                serverManager.queuePlayerForServerJoin(event.getPlayer(), originalServerName);
                serverManager.startServer(originalServer).exceptionally(ex -> {
                    // Check if the is already connected to a server
                    Optional<ServerConnection> playerCurrentServer = event.getPlayer().getCurrentServer();
                    if (playerCurrentServer.isEmpty()) {
                        event.getPlayer().disconnect(Component.text("Failed to start server " + originalServerName).color(NamedTextColor.RED));
                    } else {
                        // send fail message
                        sendMessageToPlayer(event.getPlayer(), config.getMessage("failed").orElse(""), originalServerName);
                    }
                    return null;
                });
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error occurred while determining the status of server {}", originalServerName);
            logger.error("Exception: {}", e.getMessage(), e);
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
        }


        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        logger.debug("onServerPreConnect completed in: {}", duration);
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        logger.trace("{}ServerPostConnectEvent: {} {}", AnsiColors.CYAN, event, AnsiColors.RESET);

        RegisteredServer previousServer = event.getPreviousServer();

        if (previousServer != null) {
            serverManager.scheduleShutdownServer(previousServer, false);
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        logger.trace("{}DisconnectEvent: {} {}", AnsiColors.CYAN, event, AnsiColors.RESET);

        Player player = event.getPlayer();
        Optional<ServerConnection> serverConnection = player.getCurrentServer();
        serverConnection.ifPresent(connection -> serverManager.scheduleShutdownServer(connection.getServer(), true));
    }

    @Subscribe
    public void onPlayerKicked(KickedFromServerEvent event) {
        logger.trace("{}KickedFromServerEvent: {} {}", AnsiColors.CYAN, event, AnsiColors.RESET);

        Player player = event.getPlayer();
        RegisteredServer server = event.getServer();
        String kickedFrom = server.getServerInfo().getName();

        logger.debug("{} was kicked from {} for {}", player.getUsername(), kickedFrom, event.getServerKickReason().orElse(null));

        serverManager.scheduleShutdownServer(server, false);
    }

    public AutoServerLogger getLogger() {
        return logger;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Configuration getConfig() {
        return config;
    }

    public ServerManager getServerManager() {
        return serverManager;
    }

    public Optional<String> getVersion() {
        return pluginContainer.getDescription().getVersion();
    }

    public String getSecret() {
        logger.trace("Getting secret from file.");
        try {
            return new String(Files.readAllBytes(Paths.get("forwarding.secret"))).trim();
        } catch (IOException e) {
            logger.error("Failed to get secret: " + e.getMessage());
        }
        return null;
    }

    private void notifyUpdates() {
        Optional<String> versionOptional = getVersion();
        if (versionOptional.isPresent()) {
            try {
                String currentVersion = versionOptional.get();
                UpdateChecker updateChecker = new UpdateChecker(logger, currentVersion);

                if (updateChecker.isUpdateAvailable()) {
                    logger.info("======================================== ");
                    logger.info(" A new update is available!");
                    logger.info(" Current Version: {}", currentVersion);
                    logger.info(" Latest Version: {}", updateChecker.latest());
                    logger.info(" Download the latest version for new features and fixes.");
                    logger.info("========================================");
                } else {
                    logger.info("You are using the latest version ({}). No updates available.", currentVersion);
                }
            } catch (Exception ignored) {
                logger.warn("Could not check for updates, check your connection.");
            }
        } else {
            logger.warn("Unable to determine the current version. Update check skipped.");
        }
    }
}
