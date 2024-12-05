package me.artificial.autoserver.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import java.util.List;

public interface SubCommand {
    void execute(CommandSource source, String[] args);

    boolean hasPermission(SimpleCommand.Invocation invocation);

    List<String> suggest(SimpleCommand.Invocation invocation);
}
