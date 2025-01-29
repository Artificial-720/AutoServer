package me.artificial.autoserver.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.artificial.autoserver.velocity.AutoServer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;

public class StartCommand implements SubCommand {
    private final AutoServer plugin;

    public StartCommand(AutoServer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSource source, String[] args) {
        if (args.length != 2) {
            source.sendMessage(Component.text().content("Usage /autoserver start <serverName>"));
            return;
        }

        String serverName = args[1];
        Optional<RegisteredServer> optionalServer = plugin.getProxy().getServer(serverName);
        if (optionalServer.isEmpty()) {
            source.sendMessage(Component.text().content("Server \"" + serverName + "\" not found. Please check the server name and try again"));
            return;
        }
        RegisteredServer server = optionalServer.get();
        source.sendMessage(Component.text().content("Starting server \"" + serverName + "\"... Please wait."));

        plugin.getServerManager().startServer(server).whenComplete((result, ex) -> {
            if (ex != null) { // error occurred
                plugin.getLogger().info("Error: {}", ex.getMessage());
                source.sendMessage(Component.text().content(ex.getMessage()));
            } else {
                plugin.getLogger().info("Message: {}", result);
                source.sendMessage(Component.text().content(result));
            }
        });
    }

    @Override
    public boolean hasPermission(SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("autoserver.command.start");
    }

    @Override
    public List<String> suggest(SimpleCommand.Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 2) {
            String part = args[1].toLowerCase();
            return plugin.getProxy().getAllServers().stream()
                    .map(s -> s.getServerInfo().getName())
                    .filter(name -> name.toLowerCase().startsWith(part)).toList();
        }
        return List.of();
    }
}
