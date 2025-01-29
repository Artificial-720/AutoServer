package me.artificial.autoserver.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;

import java.util.List;

public class HelpCommand implements SubCommand {

    @Override
    public void execute(CommandSource source, String[] args) {
        source.sendMessage(Component.text("""
                usage: autoserver <command> [<args>]
                
                   reload    Reloads the config file
                   help      This message
                   status    Show status of a server
                   start     Run the start sequence for a server
                   info      Show more details about the server
                """));
    }

    @Override
    public boolean hasPermission(SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("autoserver.command.help");
    }

    @Override
    public List<String> suggest(SimpleCommand.Invocation invocation) {
        return List.of();
    }
}
