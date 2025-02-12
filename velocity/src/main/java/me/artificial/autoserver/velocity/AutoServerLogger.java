package me.artificial.autoserver.velocity;

import org.slf4j.Logger;
import org.slf4j.event.Level;

/*
- **Trace** – Used for detailed debugging when tracking a specific part of a function.
- **Debug** – Diagnostic information helpful for developers, IT, and sysadmins.
- **Info** – General operational messages (e.g., service start/stop) that are useful but not critical.
- **Warn** – Recoverable issues that might cause unexpected behavior (e.g., switching to a backup server).
- **Error** – Serious issues requiring user intervention but not causing a full system failure.
 */

public class AutoServerLogger {
    private final Logger logger;
    private final AutoServer plugin;

    public AutoServerLogger(AutoServer plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void trace(String msg, Object... args) {
        log(Level.TRACE, msg, args);
    }

    public void debug(String msg, Object... args) {
        log(Level.DEBUG, msg, args);
    }

    public void info(String msg, Object... args) {
        log(Level.INFO, msg, args);
    }

    public void warn(String msg, Object... args) {
        log(Level.WARN, msg, args);
    }

    public void error(String msg, Object... args) {
        log(Level.ERROR, msg, args);
    }

    private void log(Level level, String message, Object... args) {
        if (shouldLog(level)) {
            switch (level) {
                case TRACE -> logger.trace(message, args);
                case DEBUG -> logger.debug(message, args);
                case INFO -> logger.info(message, args);
                case WARN -> logger.warn(message, args);
                case ERROR -> logger.error(message, args);
            }
        }
    }

    private boolean shouldLog(Level level) {
        String currentLevel = plugin.getConfig().getLogLevel();
        Level logLevel = Level.valueOf(currentLevel);
        return level.toInt() >= logLevel.toInt();
    }
}
