package me.artificial.autoserver.fabric;

import me.artificial.autoserver.common.CommandRunner;
import me.artificial.autoserver.common.Config;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.util.logging.Logger;

public class AutoServerFabric implements ModInitializer {

    private static final Logger logger = Logger.getLogger(AutoServerFabric.class.getName());
    private static final String ID = "autoserverfabric";
    private String command;

    @Override
    public void onInitialize() {
        command = Config.getProperty("runJarCommand");
        if (command == null) {
            logger.warning("Error reading runJarCommand.");
        } else {
            logger.info("Successfully enabled AutoServer.");
        }

        // Register server stop event
        Runtime.getRuntime().addShutdownHook(new Thread(this::onDisable));
    }

    @Environment(EnvType.SERVER)
    public void onDisable() {
        try {
            // Get the path to the mod's JAR file
            File modFile = FabricLoader.getInstance().getModContainer(ID)
                    .map(container -> container.getOrigin().getPaths()
                            .getFirst().toFile()).orElseThrow(() -> new RuntimeException("Unable to locate mod file."));

            String pluginPath = "\"" + modFile.getAbsolutePath().replace("\\", "/") + "\"";
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
