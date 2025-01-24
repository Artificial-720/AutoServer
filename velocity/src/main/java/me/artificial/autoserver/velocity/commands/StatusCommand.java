package me.artificial.autoserver.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.artificial.autoserver.velocity.AutoServer;
import me.artificial.autoserver.velocity.ServerStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.Optional;

public class StatusCommand implements SubCommand {
    private final AutoServer plugin;

    public StatusCommand(AutoServer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSource source, String[] args) {
        TextComponent.Builder builder = Component.text().content("Server Status").decorate(TextDecoration.BOLD);

        if (args.length == 2) {
            if (!handleSingleServer(source, args[1], builder)) {
                return;
            }
        } else {
            handleAllServers(builder);
        }

        source.sendMessage(builder.build());
    }

    private boolean handleSingleServer(CommandSource source, String serverName, TextComponent.Builder builder) {
        Optional<RegisteredServer> optionalServer = plugin.getProxy().getServer(serverName);
        if (optionalServer.isPresent()) {
            ServerStatus status = plugin.getServerManager().getServerStatus(optionalServer.get());
            TextColor color = getColorFromStatus(status);
            builder.appendNewline().append(Component.text().content(serverName).color(color));
        } else {
            plugin.getLogger().info("Error getting server, name is probably wrong");
            source.sendMessage(Component.text().content("Failed to check status, double check spelling."));
            return false;
        }
        return true;
    }

    private void handleAllServers(TextComponent.Builder builder) {
        for (RegisteredServer server : plugin.getProxy().getAllServers()) {
            ServerStatus status = plugin.getServerManager().getServerStatus(server);
            TextColor color = getColorFromStatus(status);
            builder.appendNewline().append(Component.text().content(server.getServerInfo().getName()).color(color));
        }
    }

    private TextColor getColorFromStatus(ServerStatus status) {
        return switch (status) {
            case ServerStatus.RUNNING -> NamedTextColor.GREEN;
            case ServerStatus.STOPPED -> NamedTextColor.GRAY;
            case ServerStatus.UNKNOWN -> NamedTextColor.RED;
        };
    }

    @Override
    public boolean hasPermission(SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("autoserver.command.status");
    }

    @Override
    public List<String> suggest(SimpleCommand.Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 2) {
            String part = args[1].toLowerCase();
            return plugin.getProxy().getAllServers().stream().map(s -> s.getServerInfo().getName()).filter(name -> name.toLowerCase().startsWith(part)).toList();
        }
        return List.of();
    }
}
