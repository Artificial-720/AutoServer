package me.artificial.autoserver.fabric;

import me.artificial.autoserver.common.CommandRunner;
import me.artificial.autoserver.common.Config;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.util.logging.Logger;

public class AutoServerFabric implements ModInitializer {

    private static final Logger logger = Logger.getLogger(AutoServerFabric.class.getName());
    private static final String ID = "autoserverfabric";
    private String command;
    private String pluginPath;

    @Override
    public void onInitialize() {
        try {
            command = Config.getProperty("runJarCommand");
            if (command == null) {
                logger.warning("Error: 'runJarCommand' property is missing or empty in the configuration.");
                return;
            } else {
                logger.info("Successfully loaded 'runJarCommand': " + command);
            }

            pluginPath = getPluginPath();
            if (pluginPath == null) {
                logger.warning("Unable to determine plugin path. Features requiring will not work.");
                return;
            }

            // Register server stop event
            Runtime.getRuntime().addShutdownHook(new Thread(this::onDisable));

            logger.info("Successfully enabled AutoServer.");
        } catch (Exception e) {
            logger.severe("An error occurred during initialization.");
        }
    }

    private String getPluginPath() {
        // Get the path to the mod's JAR file
        File modFile = FabricLoader.getInstance().getModContainer(ID)
                .map(container -> container.getOrigin().getPaths()
                        .getFirst().toFile()).orElse(null);
        if (modFile == null) {
            logger.warning("Mod file not found. This may indicate a misconfiguration or loading issue.");
            return null;
        }
        return "\"" + modFile.getAbsolutePath().replace("\\", "/") + "\"";
    }

    public void onDisable() {
        try {
            String replacedCommand = command.replace("%pluginPath%", pluginPath);

            if (CommandRunner.runCommand(replacedCommand)) {
                logger.info("Successfully started Boot Listener.");
            } else {
                logger.warning("Failed to start Boot Listener");
            }
        } catch (Exception e) {
            logger.severe("An error occurred during shutdown: " + e.getMessage());
        }

        logger.info("Successfully disabled AutoServer");
    }
}
