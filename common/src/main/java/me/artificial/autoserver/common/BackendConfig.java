package me.artificial.autoserver.common;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

public class BackendConfig {
    public static final String DIRECTORY_NAME_DEFAULT = "AutoServer";
    private static final String CONFIG_FILE = "config.yml";

    private final File configDirectory;
    private Map<String, Object> data;

    public BackendConfig(File configDirectory) {
        this.configDirectory = configDirectory;
        reload();
    }

    public void reload() {
        File configFile = new File(configDirectory, CONFIG_FILE);
        System.out.println("Config file: " + configFile.getPath());

        if (!configFile.exists()) {
            System.out.println("Config file does not exist");
            if (!configDirectory.mkdirs()) {
                System.err.println("Failed to create config directory.");
                return;
            }
            extractDefaultConfig(configFile);
        }

        InputStream inputStream;
        try {
            inputStream = new FileInputStream(configFile);
            Yaml yaml = new Yaml();
            data = yaml.load(inputStream);
            System.out.println(data);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public String getString(String path) {
        Object value = followPath(path);
        return (value instanceof String) ? (String) value : null;
    }

    public Boolean getBoolean(String path) {
        return getBoolean(path, null);
    }

    public Boolean getBoolean(String path, Boolean def) {
        Object value = followPath(path);
        return (value instanceof Boolean) ? (Boolean) value : def;
    }

    public Integer getInt(String path) {
        Object value = followPath(path);
        return (value instanceof Integer) ? (Integer) value : null;
    }

    public String getConfigPath() {
        return configDirectory.getPath();
    }

    private Object followPath(String path) {
        String[] keys = path.split("\\.");

        Object value = data;

        for (String key: keys) {
            if (value instanceof Map) {
                value = ((Map<?, ?>) value).get(key);
            } else {
                return null;
            }
        }

        return value;
    }

    private void extractDefaultConfig(File configFile) {
        System.out.println("Creating file");
        try (InputStream inputStream = BackendConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
             FileOutputStream outputStream = new FileOutputStream(configFile)) {

            if (inputStream == null) {
                System.err.println("Resource not found: " + CONFIG_FILE);
                return;
            }

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            System.out.println("File extracted: " + configFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("We had an error while opening the resource: " + e.getMessage());
        }
    }
}
