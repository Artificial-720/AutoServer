package me.artificial.autoserver.velocity;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.time.Instant;

public class ServerStatus {

    public enum Status {
        RUNNING,
        STOPPED,
        STARTING,
        STOPPING,
        UNKNOWN
    }
    private Status status;
    private Instant timestamp;

    public ServerStatus() {
        setStatus(Status.UNKNOWN);
    }

    public String getDisplayName() {
        return switch (status) {
            case RUNNING -> "Online";
            case STOPPED -> "Offline";
            case STARTING -> "Starting";
            case STOPPING -> "Stopping";
            case UNKNOWN -> "Unknown";
        };
    }

    public TextColor getColor() {
        return switch (status) {
            case RUNNING -> NamedTextColor.GREEN;
            case STOPPED -> NamedTextColor.GRAY;
            case STARTING -> NamedTextColor.YELLOW;
            case STOPPING -> NamedTextColor.BLUE;
            case UNKNOWN -> NamedTextColor.RED;
        };
    }

    public void setStatus(Status status) {
        this.timestamp = Instant.now();
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isStopping() {
        return status.equals(Status.STOPPING);
    }

    public boolean isStarting() {
        return status.equals(Status.STARTING);
    }

    public boolean is(Status status) {
        return this.status.equals(status);
    }

    @Override
    public String toString() {
        return getDisplayName() + " (Set at: " + timestamp + ")";
    }
}