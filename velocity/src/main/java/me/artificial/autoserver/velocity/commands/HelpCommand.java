package me.artificial.autoserver.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.List;

public class HelpCommand implements SubCommand {
    private final HashMap<String, SubCommand> subCommands;

    public HelpCommand(HashMap<String, SubCommand> subCommands) {
        this.subCommands = subCommands;
    }

    @Override
    public void execute(CommandSource source, String[] args) {
        // no usage check for help command

        source.sendMessage(Component.text(fullHelpMessage()));
    }

    @Override
    public boolean hasPermission(SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("autoserver.command.help");
    }

    @Override
    public List<String> suggest(SimpleCommand.Invocation invocation) {
        return List.of();
    }

    @Override
    public String help() {
        return "This message";
    }

    private String fullHelpMessage() {
        StringBuilder sb = new StringBuilder("Usage: /autoserver <command> [<args>]");
        subCommands.forEach((command, subCommand) -> {
            sb.append('\n');
            sb.append(String.format("   %-10s %s", command, subCommand.help()));
        });
        return sb.toString();
    }
}
