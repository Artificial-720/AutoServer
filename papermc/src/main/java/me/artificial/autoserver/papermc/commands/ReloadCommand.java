package me.artificial.autoserver.papermc.commands;

import me.artificial.autoserver.papermc.AutoServerPaper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ReloadCommand implements SubCommand{
    private final AutoServerPaper plugin;

    public ReloadCommand(AutoServerPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        plugin.reloadConfigValues();
        sender.sendMessage(Component.text("Configuration reloaded successfully.").color(NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return List.of();
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("autoserver.reload");
    }
}
