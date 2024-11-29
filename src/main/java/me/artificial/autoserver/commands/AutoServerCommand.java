package me.artificial.autoserver.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import me.artificial.autoserver.AutoServer;
import net.kyori.adventure.text.Component;

import java.util.HashMap;

public class AutoServerCommand implements SimpleCommand {
    private final AutoServer plugin;
    private final HashMap<String, SubCommand> subCommands;

    public AutoServerCommand(AutoServer plugin) {
        this.plugin = plugin;
        subCommands = new HashMap<>();

        subCommands.put("reload", new ReloadCommand(plugin));
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            source.sendMessage(Component.text("Usage: /autoserver <start|stop|restart|autostart|autostop|list|reload>"));
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

        //return invocation.source().hasPermission("autoserver.command." + invocation.arguments()[0].toLowerCase());
    }
}
