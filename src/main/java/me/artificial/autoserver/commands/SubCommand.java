package me.artificial.autoserver.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

public interface SubCommand {
    void execute(CommandSource source, String[] args);

    boolean hasPermission(SimpleCommand.Invocation invocation);
}
