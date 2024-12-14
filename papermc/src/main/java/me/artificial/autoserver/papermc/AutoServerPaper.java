package me.artificial.autoserver.papermc;

import org.bukkit.plugin.java.JavaPlugin;

import me.artificial.autoserver.common.Config;
import me.artificial.autoserver.common.CommandRunner;

public final class AutoServerPaper extends JavaPlugin {

    private String command;

    @Override
    public void onEnable() {
        command = Config.getProperty("runPluginCommand");
        if (command == null) {
            getLogger().warning("Error reading runPluginCommand command.");
        }
        getLogger().info("Successfully enabled AutoServer");
    }

    @Override
    public void onDisable() {
        String pluginPath = "\"" + getFile().getAbsolutePath().replace("\\", "/") + "\"";
        String replacedCommand = command.replace("%pluginPath%", pluginPath);
        if (CommandRunner.runCommand(replacedCommand)) {
            getLogger().info("Successfully started Boot Listener.");
        } else {
            getLogger().info("Failed to start Boot Listener");
        }

        getLogger().info("Successfully disabled AutoServer");
    }
}
