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

            plugin.getLogger().info("Running start command for {} server. '{}'", server.getServerInfo().getName(), command.get());
            if (CommandRunner.runCommand(command.get())) {
                return "Command ran successfully";
            } else {
                plugin.getLogger().error("Failed to run start command for server {}", server.getServerInfo().getName());
                throw new RuntimeException("Failed to run command");
            }
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

            plugin.getLogger().info("Running stop command for {} server. '{}'", server.getServerInfo().getName(), command.get());
            if (CommandRunner.runCommand(command.get())) {
                return "Command ran successfully";
            } else {
                plugin.getLogger().error("Failed to run stop command for server {}", server.getServerInfo().getName());
                throw new RuntimeException("Failed to run command");
            }
        });
    }
}
