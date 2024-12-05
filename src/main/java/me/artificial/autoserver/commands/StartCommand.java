package me.artificial.autoserver.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.artificial.autoserver.AutoServer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;

public class StartCommand implements SubCommand{
    private final AutoServer plugin;

    public StartCommand(AutoServer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendMessage(Component.text("Usage: autoserver start <serverName>"));
            return;
        }

        String serverName = args[1];
        plugin.getLogger().info("Attempting to start server: {}", serverName);

        Optional<RegisteredServer> server = plugin.getProxy().getServer(serverName);
        if (server.isEmpty()) {
            plugin.getLogger().error("Failed to get Server");
            return;
        }

        plugin.getServerManager().startServer(server.get());
    }

    @Override
    public boolean hasPermission(SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("autoserver.command.start");
    }

    @Override
    public List<String> suggest(SimpleCommand.Invocation invocation) {
        String[] args = invocation.arguments();
        String part = args[1].toLowerCase();
        return plugin.getServerNames().stream()
                .filter(cmd -> cmd.startsWith(part))
                .toList();
    }
}
