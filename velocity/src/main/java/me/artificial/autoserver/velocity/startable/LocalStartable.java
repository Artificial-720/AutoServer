package me.artificial.autoserver.velocity.startable;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.artificial.autoserver.common.CommandRunner;
import me.artificial.autoserver.velocity.AnsiColors;
import me.artificial.autoserver.velocity.AutoServer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class LocalStartable implements Startable {
    private final AutoServer plugin;
    private final RegisteredServer server;

    public LocalStartable(AutoServer plugin, RegisteredServer server) {
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

            plugin.getLogger().info("Running start command for {} server. \"{}{}{}\"", server.getServerInfo().getName(), AnsiColors.YELLOW, command.get(), AnsiColors.RESET);
            return runCommand(path.orElse(null), command.get(), preserveQuotes.orElse(null));
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

            plugin.getLogger().info("Running stop command for {} server. \"{}{}{}\"", server.getServerInfo().getName(), AnsiColors.YELLOW, command.get(), AnsiColors.RESET);
            return runCommand(path.orElse(null), command.get(), preserveQuotes.orElse(null));
        });
    }

    private String runCommand(String path, String command, Boolean preserveQuotes) throws RuntimeException {
        CommandRunner.CommandResult commandResult = CommandRunner.runCommand(path, command, preserveQuotes);
        // Sleep for 1 second to give time for command to execute
        // This is only to catch immediate errors
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (commandResult.failedToStart()) {
            throw new RuntimeException(commandResult.getErrorMessage());
        }

        plugin.getLogger().debug("Command Result: {}", commandResult);
        if (commandResult.isTerminated()) {
            String out = commandResult.getProcessOutput();
            if (!out.isBlank()) {
                plugin.getLogger().info("The command exited quickly, which may indicate an error. Here is the output: {}{}{}", AnsiColors.YELLOW, out, AnsiColors.RESET);
            }
            if (commandResult.getExitCode() != 0) {
                plugin.getLogger().error("Process exited with a non-zero exit code: {}", commandResult.getExitCode());
                throw new RuntimeException("Command failed to run.");
            }
        }
        return "Command ran successfully";
    }
}
