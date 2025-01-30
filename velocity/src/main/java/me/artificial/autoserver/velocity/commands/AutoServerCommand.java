package me.artificial.autoserver.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import me.artificial.autoserver.velocity.AutoServer;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AutoServerCommand implements SimpleCommand {
    private final HashMap<String, SubCommand> subCommands;

    public AutoServerCommand(AutoServer plugin) {
        subCommands = new HashMap<>();

        subCommands.put("reload", new ReloadCommand(plugin));
        subCommands.put("help", new HelpCommand(subCommands));
        subCommands.put("status", new StatusCommand(plugin));
        subCommands.put("info", new InfoCommand(plugin));
        subCommands.put("start", new StartCommand(plugin));
        subCommands.put("version", new VersionCommand(plugin));
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            subCommands.get("help").execute(source, args);
            return;
        }

        SubCommand command = subCommands.get(args[0].toLowerCase());
        if (command != null) {
            command.execute(source, args);
        } else {
            source.sendMessage(Component.text("Unknown subcommand: " + args[0]));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
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
            String command = args[0].toLowerCase();
            if (subCommands.containsKey(command)) {
                return subCommands.get(command).suggest(invocation);
            }
        }
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        String[] args = invocation.arguments();

        if (!invocation.source().hasPermission("autoserver.base")) {
            return false;
        }

        if (args.length == 0) {
            return true;
        }

        String command = args[0];

        if (subCommands.containsKey(command)) {
            return subCommands.get(command).hasPermission(invocation);
        }

        return true;
    }
}
