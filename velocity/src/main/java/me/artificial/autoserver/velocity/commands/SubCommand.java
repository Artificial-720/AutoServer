package me.artificial.autoserver.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import java.util.List;

public interface SubCommand {

    /**
     * Executes the subcommand with the given command source and arguments.
     *
     * @param source the source executing the command (e.g., a player or console)
     * @param args   the arguments provided for the command
     */
    void execute(CommandSource source, String[] args);

    /**
     * Checks if the command source has permission to execute this subcommand.
     *
     * @param invocation the command invocation containing the source and arguments
     * @return {@code true} if the source has permission, otherwise {@code false}
     */
    boolean hasPermission(SimpleCommand.Invocation invocation);

    /**
     * Provides a list of suggestions for tab completion when using this subcommand.
     *
     * @param invocation the command invocation containing the source and arguments
     * @return a list of suggested completions
     */
    List<String> suggest(SimpleCommand.Invocation invocation);

    /**
     * Returns the help text for this subcommand.
     *
     * @return the help description of the command
     */
    String help();
}
