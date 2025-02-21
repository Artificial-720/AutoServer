package me.artificial.autoserver.fabric;

import me.artificial.autoserver.common.BackendConfig;
import me.artificial.autoserver.common.CommandRunner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.util.logging.Logger;

public class AutoServerFabric implements ModInitializer {

    public static final String MOD_ID = "autoserver-fabric";
    private static final Logger LOGGER = Logger.getLogger(MOD_ID);
    private BackendConfig config;

    @Override
    public void onInitialize() {
        // load config
        config = new BackendConfig(new File(FabricLoader.getInstance().getGameDir().resolve("mods").toFile(), BackendConfig.DIRECTORY_NAME_DEFAULT));

        // TODO Register commands

        // Register server stop event
        Runtime.getRuntime().addShutdownHook(new Thread(this::onDisable));

        LOGGER.info("Successfully enabled AutoServer Backend");
    }

    public void onDisable() {
        // We don't use LOGGER anymore because it is often unloaded by this point
        Boolean enabled = config.getBoolean("bootListener.enabled");
        if (enabled != null && enabled) {
            System.out.println("Starting Boot Listener...");
            startBootListener();
        } else {
            System.out.println("Start Boot Listener is disabled. Change enabled to true to re-enable.");
        }

        System.out.println("Successfully disabled AutoServer");
    }

    private void startBootListener() {
        String command = config.getString("bootListener.runJarCommand");
        if (command == null) {
            System.out.println("BootListener command not found. Can't start BootListener");
            return;
        }

        String directoryPath = FabricLoader.getInstance().getGameDir().resolve("mods").toFile().getAbsolutePath();
        String jarName = new File(AutoServerFabric.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();

        System.out.println("directoryPath: " + directoryPath);
        System.out.println("jarName: " + jarName);

        // Replace %jarName% with jar file name
        String replacedCommand = command.replace("%jarName%", jarName);

        Boolean preserveQuotes = config.getBoolean("bootListener.preserveQuotes");

        System.out.println("Running Command to start BootListener");
        CommandRunner.CommandResult commandResult = CommandRunner.runCommand(directoryPath, replacedCommand, preserveQuotes);
        if (commandResult.failedToStart()) {
            System.err.println(commandResult.getErrorMessage());
        }
        if (commandResult.isTerminated()) {
            String out = commandResult.getProcessOutput();
            if (!out.isBlank()) {
                System.err.println("Command exited fast might have an error here is the output: " + out);
            }
        }
    }
}
