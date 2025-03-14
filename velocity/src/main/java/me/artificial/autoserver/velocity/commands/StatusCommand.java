package me.artificial.autoserver.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.artificial.autoserver.velocity.AutoServer;
import me.artificial.autoserver.velocity.ServerStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
        if (!(args.length == 2 || args.length == 1)) {
            source.sendMessage(Component.text().content("Usage /autoserver status [serverName]"));
            return;
        }

        TextComponent.Builder builder = Component.text().content("Server Status").decorate(TextDecoration.BOLD);

        if (args.length == 2) {
            if (!handleSingleServer(builder, args[1])) {
                plugin.getLogger().warn("Error getting server, name is probably wrong");
                source.sendMessage(Component.text().content("Failed to check status, double check spelling."));
                return;
            }
        } else {
            handleAllServers(builder);
        }

        source.sendMessage(builder.build());
    }

    private boolean handleSingleServer(TextComponent.Builder builder, String serverName) {
        Optional<RegisteredServer> optionalServer = plugin.getProxy().getServer(serverName);
        if (optionalServer.isEmpty()) {
            return false;
        }
        ServerStatus status = plugin.getServerManager().getServerStatus(optionalServer.get());
        builder.appendNewline().append(Component.text().content(serverName).color(status.getColor()));
        return true;
    }

    private void handleAllServers(TextComponent.Builder builder) {
        for (RegisteredServer server : plugin.getProxy().getAllServers()) {
            ServerStatus status = plugin.getServerManager().getServerStatus(server);
            builder.appendNewline().append(Component.text().content(server.getServerInfo().getName()).color(status.getColor()));
        }
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
            return plugin.getProxy().getAllServers().stream()
                    .map(s -> s.getServerInfo().getName())
                    .filter(name -> name.toLowerCase().startsWith(part))
                    .toList();
        }
        return List.of();
    }

    @Override
    public String help() {
        return "Show status of all servers or a specific";
    }
}
