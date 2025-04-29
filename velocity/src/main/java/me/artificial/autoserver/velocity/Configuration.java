package me.artificial.autoserver.velocity;

import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class Configuration {
    private static final long DEFAULT_START_UP_DELAY = 60L;
    private static final long DEFAULT_SHUTDOWN_DELAY = 5L;
    private static final int DEFAULT_REMOTE_PORT = 8080;
    private static final long DEFAULT_COMMUNICATION_VERSION = 2L;
    private static final long DEFAULT_AUTO_SHUTDOWN_DELAY = -1L;

    private final Path dataDirectory;
    private Toml config;

    public Configuration(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.config = new Toml();
    }

    /**
     * Reloads the config from disk
     */
    public void reloadConfig() throws RuntimeException {
        config = loadConfig(dataDirectory);
    }

    public Optional<String> getMessage(String messageType) {
        String prefix = config.getString("messages.prefix", "");
        String message = config.getString("messages." + messageType);

        if (message == null) {
            return Optional.empty();
        }

        return Optional.of(prefix + message);
    }

    public Optional<String> getPath(RegisteredServer server) {
        String path = config.getString("servers." + server.getServerInfo().getName() +  ".workingDirectory");
        return Optional.ofNullable(path);
    }

    public Optional<Boolean> getPreserveQuotes(RegisteredServer server) {
        Boolean quotes = config.getBoolean("servers." + server.getServerInfo().getName() +  ".preserveQuotes");
        return Optional.ofNullable(quotes);
    }

    public Optional<String> getStartCommand(RegisteredServer server) {
        String command = config.getString("servers." + server.getServerInfo().getName() +  ".start");
        return Optional.ofNullable(command);
    }

    public Optional<String> getStopCommand(RegisteredServer server) {
        String command = config.getString("servers." + server.getServerInfo().getName() +  ".stop");
        return Optional.ofNullable(command);
    }

    public Optional<Boolean> isRemoteServer(RegisteredServer server) {
        Boolean remote = config.getBoolean("servers." + server.getServerInfo().getName() + ".remote");
        return Optional.ofNullable(remote);
    }

    public Optional<Integer> getPort(RegisteredServer server) {
        Long longPort = config.getLong("servers." + server.getServerInfo().getName() + ".port");

        if (longPort == null) {
            return Optional.of(DEFAULT_REMOTE_PORT);
        }
        if (longPort < 0 || longPort > 65535) {
            return Optional.empty();
        }

        return Optional.of(longPort.intValue());
    }

    public boolean getSecurity(RegisteredServer server) {
        return config.getBoolean("servers." + server.getServerInfo().getName() + ".security", true);
    }

    public long getStartUpDelay(RegisteredServer server) {
        return config.getLong("servers." + server.getServerInfo().getName() + ".startupDelay", DEFAULT_START_UP_DELAY);
    }

    public long getShutdownDelay(RegisteredServer server) {
        return config.getLong("servers." + server.getServerInfo().getName() + ".shutdownDelay", DEFAULT_SHUTDOWN_DELAY);
    }

    public long getAutoShutdownDelay(RegisteredServer server) {
        return config.getLong("servers." + server.getServerInfo().getName() + ".autoShutdownDelay", DEFAULT_AUTO_SHUTDOWN_DELAY);
    }

    public boolean checkForUpdate() {
        return config.getBoolean("checkForUpdates", true);
    }

    public String getLogLevel() {
        return config.getString("logging.level", "INFO").toUpperCase();
    }

    public int getCommunicationVersion(RegisteredServer server) {
        return config.getLong("servers." + server.getServerInfo().getName() + ".communicationVersion", DEFAULT_COMMUNICATION_VERSION).intValue();
    }

    public long getMaintenanceInterval() {
        return config.getLong("maintenanceInterval", 5L);
    }

    public long StartRateLimit() {
        return config.getLong("startRateLimit", 2L);
    }

    private Toml loadConfig(Path path) throws RuntimeException {
        File configFile = new File(path.toFile(), "config.toml");

        try {
            if (!configFile.exists()) {
                if (!configFile.getParentFile().exists()) {
                    if (!configFile.getParentFile().mkdirs()) {
                        throw new IOException("Failed to create parent directories for config file.");
                    }
                }
                InputStream input = getClass().getResourceAsStream("/" + configFile.getName());
                if (input != null) {
                    Files.copy(input, configFile.toPath());
                } else if (!configFile.createNewFile()) {
                    throw new IOException("Failed to create a new config file.");
                }

            }
            return new Toml().read(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
