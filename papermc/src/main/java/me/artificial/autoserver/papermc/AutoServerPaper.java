package me.artificial.autoserver.papermc;

import me.artificial.autoserver.common.BackendConfig;
import me.artificial.autoserver.papermc.commands.Commander;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import me.artificial.autoserver.common.CommandRunner;

import java.io.File;

public final class AutoServerPaper extends JavaPlugin {

    private BackendConfig config;

    @Override
    public void onEnable() {
        // load config
        config = new BackendConfig(new File(getServer().getPluginsFolder().getAbsoluteFile(), BackendConfig.DIRECTORY_NAME_DEFAULT));

        // Register commands
        Commander commander = new Commander(this);
        PluginCommand pluginCommand = getCommand("autoserver");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(commander);
            pluginCommand.setTabCompleter(commander);
        }

        // start tcp server to listen
        // message velocity that backend is online

        getLogger().info("Successfully enabled AutoServer Backend");
    }

    @Override
    public void onDisable() {
        // if enabled start boot listener
        Boolean enabled = config.getBoolean("bootListener.enabled");
        if (enabled != null && enabled) {
            getLogger().info("Starting Boot Listener...");
            startBootListener();
        }

        getLogger().info("Successfully disabled AutoServer");
    }

    public void reloadConfigValues() {
        config.reload();
    }

    private void startBootListener() {
        String command = config.getString("bootListener.runJarCommand");
        if (command == null) {
            getLogger().severe("BootListener command not found. Can't start BootListener");
            return;
        }

        String directoryPath = getServer().getPluginsFolder().getAbsolutePath();
        String jarName = getFile().getName();

        // Replace %jarName% with jar file name
        String replacedCommand = command.replace("%jarName%", jarName);

        Boolean preserveQuotes = config.getBoolean("bootListener.preserveQuotes");

        getLogger().info("Running Command to start BootListener");
        CommandRunner.runCommand(directoryPath, replacedCommand, preserveQuotes);
    }
}
