package me.artificial.autoserver.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.artificial.autoserver.AutoServer;
import me.artificial.autoserver.CommandSocketClient;
import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;

public class ShutdownCommand implements SubCommand{
    private final AutoServer plugin;

    public ShutdownCommand(AutoServer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendMessage(Component.text("Usage: autoserver shutdown <serverName>"));
            return;
        }

        String serverName = args[1];
        plugin.getLogger().info("Attempting to shutdown server: {}", serverName);

        Optional<RegisteredServer> server = plugin.getProxy().getServer(serverName);
        if (server.isEmpty()) {
            plugin.getLogger().error("Failed to get Server");
            return;
        }

        plugin.getServerManager().stopServer(server.get());
    }

    @Override
    public boolean hasPermission(SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("autoserver.command.shutdown");
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
