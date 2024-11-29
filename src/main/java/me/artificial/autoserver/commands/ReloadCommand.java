package me.artificial.autoserver.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import me.artificial.autoserver.AutoServer;
import net.kyori.adventure.text.Component;

public class ReloadCommand implements SubCommand {
    private final AutoServer plugin;

    public ReloadCommand(AutoServer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSource source, String[] args) {
        plugin.getLogger().info("Reloading configuration...");
        plugin.reloadConfig();
        source.sendMessage(Component.text("Configuration reloaded."));
    }

    @Override
    public boolean hasPermission(SimpleCommand.Invocation invocation) {
        String[] args = invocation.arguments();
        if (!invocation.source().hasPermission("autoserver.command.reload")) {
            return false;
        }
        return true;
    }
}
