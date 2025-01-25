package me.artificial.autoserver.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.artificial.autoserver.velocity.commands.AutoServerCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Plugin(id = "autoserver")
public class AutoServer {

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private Configuration config;

    public ServerManager getServerManager() {
        return serverManager;
    }

    private ServerManager serverManager;

    @SuppressWarnings("unused")
    @Inject
    public AutoServer(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        // DON'T ACCESS VELOCITY API HERE
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
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
        config = new Configuration(dataDirectory);
        logger.info("Configuration Loaded");

        serverManager = new ServerManager(this);

        CommandManager commandManager = proxy.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("autoserver").aliases("as").plugin(this).build();
        proxy.getCommandManager().register(commandMeta, new AutoServerCommand(this));

        serverManager.refreshServerCache(proxy.getAllServers());
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
        //RegisteredServer previousServer = event.getPreviousServer(); // Server was connected too
        String originalServerName = originalServer.getServerInfo().getName();
        logger.info("Player {} attempting to join {}", event.getPlayer().getUsername(), originalServerName);

        CompletableFuture<Boolean> isOnline = serverManager.isServerOnline(originalServer);
        try {
            if (isOnline.get()) {
                logger.info("Server {}{}{} is online allowing connection", GREEN, originalServerName, RESET);
                event.setResult(ServerPreConnectEvent.ServerResult.allowed(originalServer));
            } else {
                // server not online need to start it
                logger.info("Server {}{}{} is not online attempting to start server", RED, originalServerName, RESET);
                event.setResult(ServerPreConnectEvent.ServerResult.denied());

                sendMessageToPlayer(event.getPlayer(), config.getMessage("starting").orElse(""), originalServerName);
                serverManager.delayedPlayerJoin(event.getPlayer(), originalServerName);
                serverManager.startServer(originalServer);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Exception: {}", e.getMessage(), e);
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
        }


        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        logger.info("onServerPreConnect completed in: {}", duration);
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Configuration getConfig() {
        return config;
    }
}
