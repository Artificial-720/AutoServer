package me.artificial.autoserver.papermc.commands;

import me.artificial.autoserver.papermc.AutoServerPaper;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Commander implements CommandExecutor, TabCompleter {
    private final HashMap<String, SubCommand> subCommands;

    public Commander(AutoServerPaper plugin) {
        subCommands = new HashMap<>();

        subCommands.put("reload", new ReloadCommand(plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /autoserver <subcommand>"));
            return true;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null) {
            sender.sendMessage(Component.text("Unknown subcommand."));
            return true;
        }

        if (!subCommand.hasPermission(sender)) {
            sender.sendMessage(Component.text("You do not have permission to use this command!"));
            return true;
        }

        return subCommand.execute(sender, args);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> commands = new ArrayList<>(subCommands.keySet());
        if (args.length == 0) {
            // nothing typed yet
            return commands;
        } else if (args.length == 1) {
            String part = args[0].toLowerCase();
            return subCommands.keySet().stream()
                    .filter(cmd -> cmd.startsWith(part))
                    .toList();
        } else if (args.length == 2) {
            // 2 args pass onto the sub command for suggestions
            String subCommand = args[0].toLowerCase();
            if (subCommands.containsKey(subCommand)) {
                return subCommands.get(subCommand).onTabComplete(sender, command, label, args);
            }
        }
        return List.of();
    }
}
