package me.artificial.autoserver.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import me.artificial.autoserver.velocity.AutoServer;
import net.kyori.adventure.text.Component;

import java.util.List;

public class ReloadCommand implements SubCommand {
    private final AutoServer plugin;

    public ReloadCommand(AutoServer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSource source, String[] args) {
        if (args.length != 1) {
            source.sendMessage(Component.text().content("Usage /autoserver reload"));
            return;
        }

        plugin.getLogger().info("Reloading configuration...");
        plugin.getConfig().reloadConfig();
        plugin.getLogger().info("Configuration reloaded.");
        source.sendMessage(Component.text("Configuration reloaded."));
    }

    @Override
    public boolean hasPermission(SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("autoserver.command.reload");
    }

    @Override
    public List<String> suggest(SimpleCommand.Invocation invocation) {
        return List.of();
    }

    @Override
    public String help() {
        return "Reloads the config file";
    }
}
