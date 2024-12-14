package me.artificial.autoserver.common;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class Config {
    public static final String CONFIG_DIR = "./config";
    public static final String CONFIG_FILE = "AutoServer.properties";
    private static final Properties properties = new Properties();

    static {
        loadConfig();
    }

    public static void saveConfig(String comments) {
        try (FileWriter writer = new FileWriter(new File(CONFIG_DIR, CONFIG_FILE))) {
            properties.store(writer, comments);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadConfig() {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, CONFIG_FILE);
        System.out.println("Config path: " + file.getAbsolutePath());
        if (!file.exists()) {
            loadDefaultConfig();
        } else {
            try (FileReader reader = new FileReader(file)) {
                properties.load(reader);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("Loaded Config Properties: " + properties);
    }

    private static void loadDefaultConfig() {
        setProperty("port", "8080");
        setProperty("startCommand", "./run.sh");
        setProperty("runPluginCommand", "~/.jdks/corretto-22.0.2/bin/java -jar %pluginPath%");
        saveConfig(" Default configuration file for the AutoServer.\n"
                + " port: The port on which the Boot Listener listens for incoming connections. Default is 8080.\n"
                + " startCommand: The script or command used to start the server. Default is run.sh.\n"
                + " runPluginCommand: Command template to execute the Boot Listener. The placeholder %pluginPath% will be replaced with the absolute path to the plugin jar.");
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

}