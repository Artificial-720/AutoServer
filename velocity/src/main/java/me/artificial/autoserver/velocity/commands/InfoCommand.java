package me.artificial.autoserver.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.artificial.autoserver.velocity.AutoServer;
import me.artificial.autoserver.velocity.ServerStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.Optional;

public class InfoCommand implements SubCommand {
    private final AutoServer plugin;

    public InfoCommand(AutoServer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSource source, String[] args) {
        if (args.length != 2) {
            source.sendMessage(Component.text().content("Usage /autoserver info <serverName>"));
            return;
        }

        Optional<RegisteredServer> optionalServer = plugin.getProxy().getServer(args[1]);
        if (optionalServer.isEmpty()) {
            source.sendMessage(Component.text().content("Unknown server name. Double check spelling."));
            return;
        }
        RegisteredServer server = optionalServer.get();
        ServerStatus serverStatus = plugin.getServerManager().getServerStatus(server);
        String message = buildMessage(server, serverStatus);

        source.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    private String buildMessage(RegisteredServer server, ServerStatus serverStatus) {
        String statusColor = getColorFromStatus(serverStatus);
        String status = statusToString(serverStatus);

        String message = """
                <bold>Server Info: <aqua>%s</aqua></bold>
                <gray>--------------------------------------</gray>
                Status: <%s>%s</%s>
                IP: <gold>%s</gold>
                Port: <gold>%d</gold>
                <gray>--------------------------------------</gray>
                """;
        message = String.format(message,
                server.getServerInfo().getName(),
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

    @Override
    public String help() {
        return "Show more details about the server";
    }

    private String statusToString(ServerStatus status) {
        return switch (status) {
            case ServerStatus.RUNNING -> "Online";
            case STOPPED -> "Offline";
            case STARTING -> "Starting";
            case UNKNOWN -> "Unknown";
        };
    }

    private String getColorFromStatus(ServerStatus status) {
        return switch (status) {
            case ServerStatus.RUNNING -> "green";
            case ServerStatus.STOPPED -> "gray";
            case ServerStatus.STARTING -> "yellow";
            case ServerStatus.UNKNOWN -> "red";
        };
    }
}
