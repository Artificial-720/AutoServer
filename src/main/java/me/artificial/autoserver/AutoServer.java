package me.artificial.autoserver;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import me.artificial.autoserver.commands.AutoServerCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import javax.naming.ConfigurationException;
import java.net.InetAddress;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Plugin(id = "autoserver", name = "AutoServer", version = BuildConstants.VERSION, authors = "Artificial-720")
public class AutoServer {

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private Toml config;
    private ServerManager serverManager;

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
        if (serverName != null) {
            message = message.replace("%servername%", serverName);
        }
        MiniMessage miniMessage = MiniMessage.miniMessage();
        Component component = miniMessage.deserialize(message);
        player.sendMessage(component);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // velocity starting up (register event listeners here)
        logger.info("Loading configuration...");
        config = loadConfig(dataDirectory);
        logger.info("Configuration Loaded");

        serverManager = new ServerManager(this);

        CommandManager commandManager = proxy.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("autoserver")
                .aliases("as")
                .plugin(this)
                .build();
        proxy.getCommandManager().register(commandMeta, new AutoServerCommand(this));

        logger.info("Successfully enabled AutoServer");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        // TODO Maybe shutdown all servers
        logger.info("Successfully disabled AutoServer");
    }

    // Server Events
    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        // Check if the target server should be started
        RegisteredServer originalServer = event.getOriginalServer(); // Server trying to connect too
        //RegisteredServer previousServer = event.getPreviousServer(); // Server was connected too
        CompletableFuture<ServerPing> serverPing = originalServer.ping();
        String originalServerName = originalServer.getServerInfo().getName();

        // TODO Stop any scheduled shutdown for original server

        logger.info("Player {} attempting to join {}", event.getPlayer().getUsername(), originalServerName);

        try {
            // Check if original server is online by pinging it
            serverPing.get();
            // Server is online allow player to connect
            logger.info("Server {}{}{} is online allowing connection", GREEN, originalServerName, RESET);
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(originalServer));
        } catch (ExecutionException e) {
            // Server is not online
            // Deny connection initially until server is online
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            logger.info("Server {}{}{} is not online attempting to start server", RED, originalServerName, RESET);
            sendMessageToPlayer(event.getPlayer(), getStartingMessage(), originalServerName);
            serverManager.delayedPlayerJoin(event.getPlayer(), originalServerName);
            serverManager.startServer(originalServer);
        } catch (InterruptedException e) {
            // Something didn't work
            logger.error("Error during server connection");
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
        }
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        // Logic when a player connects to a backend server
        // logger.info("onServerConnected Event {}", event.toString());
    }

    public Logger getLogger() {
        return logger;
    }

    public String getStartingMessage() {
        return config.getString("starting_message");
    }
    public String getNotifyMessage() {
        return config.getString("notify_message");
    }
    public String getFailedMessage() {
        return config.getString("failed_message");
    }

    public String getStartCommand(String serverName) throws ConfigurationException {
        if (serverName == null || serverName.isEmpty()) {
            throw new IllegalArgumentException("Server name cannot be null or empty");
        }

        Toml serversTable = config.getTable("servers");
        if (serversTable == null) {
            throw new ConfigurationException("Missing 'servers' table in the TOML configuration");
        }
        Toml specificTable = serversTable.getTable(serverName);
        if (specificTable == null) {
            throw new ConfigurationException("No configuration found for server: " + serverName);
        }
        String command = specificTable.getString("start");
        if (command == null) {
            throw new ConfigurationException("Missing 'start' command for server: " + serverName);
        }
        return command;
    }

    // ------------------------------------------------------------
    // Private functions
    // ------------------------------------------------------------

    private Toml loadConfig(Path path) {
        File configFile = new File(path.toFile(), "config.toml");

        try {
            if (!configFile.exists()) {
                if (!configFile.getParentFile().exists()) {
                    if (!configFile.getParentFile().mkdirs()) {
                        throw new IOException("Failed to create parent directories for config file.");
                    }
                }
                InputStream input = getClass().getResourceAsStream("/" + configFile.getName());
                if (input != null) {
                    Files.copy(input, configFile.toPath());
                } else if (!configFile.createNewFile()) {
                    throw new IOException("Failed to create a new config file.");
                }

            }
            return new Toml().read(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public void reloadConfig() {
        config = loadConfig(dataDirectory);
    }

    public boolean isRemoteServer(RegisteredServer server) {
        InetAddress address = server.getServerInfo().getAddress().getAddress();
        return !address.isLoopbackAddress() && !address.isAnyLocalAddress();
    }
}
