package me.artificial.autoserver.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import me.artificial.autoserver.velocity.AutoServer;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.Optional;

public class VersionCommand implements SubCommand {

    private final String version;

    public VersionCommand(AutoServer plugin) {
        Optional<String> versionOptional = plugin.getVersion();
        this.version = versionOptional.orElse("Unknown");
    }

    @Override
    public void execute(CommandSource source, String[] args) {
        source.sendMessage(MiniMessage.miniMessage().deserialize(
                "<white>Version: </white><yellow>" + version + "</yellow>"
        ));
    }

    @Override
    public boolean hasPermission(SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("autoserver.command.version");
    }

    @Override
    public List<String> suggest(SimpleCommand.Invocation invocation) {
        return List.of();
    }
}
