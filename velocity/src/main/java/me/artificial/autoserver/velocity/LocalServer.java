package me.artificial.autoserver.velocity;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.artificial.autoserver.common.CommandRunner;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class LocalServer implements Server {
    private final AutoServer plugin;
    private final RegisteredServer server;

    LocalServer(AutoServer plugin, RegisteredServer server) {
        this.plugin = plugin;
        this.server = server;
    }

    @Override
    public CompletableFuture<String> start() {
        return CompletableFuture.supplyAsync(() -> {
            Optional<String> command = plugin.getConfig().getStartCommand(server);
            if (command.isEmpty()) {
                plugin.getLogger().error("Start command not found for {}", server.getServerInfo().getName());
                throw new RuntimeException("Command not found");
            }
            Optional<String> path = plugin.getConfig().getPath(server);
            Optional<Boolean> preserveQuotes = plugin.getConfig().getPreserveQuotes(server);

            plugin.getLogger().info("Running start command for {} server. '{}'", server.getServerInfo().getName(), command.get());
            CommandRunner.runCommand(path.orElse(null), command.get(), preserveQuotes.orElse(null));
            return "Command ran successfully";
        });
    }

    @Override
    public CompletableFuture<String> stop() {
        return CompletableFuture.supplyAsync(() -> {
            Optional<String> command = plugin.getConfig().getStopCommand(server);
            if (command.isEmpty()) {
                plugin.getLogger().error("Stop command not found for {}", server.getServerInfo().getName());
                throw new RuntimeException("Command not found");
            }
            Optional<String> path = plugin.getConfig().getPath(server);
            Optional<Boolean> preserveQuotes = plugin.getConfig().getPreserveQuotes(server);

            plugin.getLogger().info("Running stop command for {} server. '{}'", server.getServerInfo().getName(), command.get());
            CommandRunner.runCommand(path.orElse(null), command.get(), preserveQuotes.orElse(null));
            return "Command ran successfully";
        });
    }
}
