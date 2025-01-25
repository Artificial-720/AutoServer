package me.artificial.autoserver.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.artificial.autoserver.velocity.AutoServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class InfoCommand implements SubCommand {
    private final AutoServer plugin;

    public InfoCommand(AutoServer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSource source, String[] args) {
        if (args.length == 2) {
            String serverName = args[1];
            Optional<RegisteredServer> optionalServer = plugin.getProxy().getServer(serverName);
            if (optionalServer.isEmpty()) {
                source.sendMessage(Component.text().content("Server not found"));
                return;
            }
            RegisteredServer server = optionalServer.get();
            CompletableFuture<Boolean> isOnlineFuture = plugin.getServerManager().isServerOnline(server);
            boolean isOnline = false;
            try {
                isOnline = isOnlineFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                plugin.getLogger().error("Error getting status of server.");
            }

            String message = buildMessage(isOnline, serverName, server);

            source.sendMessage(MiniMessage.miniMessage().deserialize(message));
        } else {
            source.sendMessage(Component.text().content("Unknown server name. Double check spelling."));
        }
    }

    private String buildMessage(boolean isOnline, String serverName, RegisteredServer server) {
        String statusColor = isOnline ? "green" : "red";
        String status = isOnline ? "Online" : "Offline";

        String message = """
                <bold>Server Info: <aqua>%s</aqua></bold>
                <gray>--------------------------------------</gray>
                Status: <%s>%s</%s>
                IP: <gold>%s</gold>
                Port: <gold>%d</gold>
                <gray>--------------------------------------</gray>
                """;
        message = String.format(message,
                serverName,
                statusColor, status, statusColor,
                server.getServerInfo().getAddress().getAddress(),
                server.getServerInfo().getAddress().getPort());
        return message;
    }

    @Override
    public boolean hasPermission(SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("autoserver.command.info");
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
