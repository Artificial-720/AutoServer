package me.artificial.autoserver.velocity;

public class NetworkCommands {
    /*
     * AutoServer -> Backend
     */
    /** BOOT backend server */
    public static final String BOOT = "BOOT_SERVER";
    /** Stop backend listener */
    public static final String SHUTDOWN_BOOT_LISTENER = "SHUTDOWN_BOOT_LISTENER";

    /*
     * Backend -> AutoServer
     */
    /** Backend server received the boot command and has acknowledged it */
    public static final String ACKNOWLEDGED = "ACKNOWLEDGED";
    /** Backend server has executed the boot command successfully but is not yet running */
    public static final String COMPLETED = "COMPLETED";
    /** Backend server encountered an error during boot */
    public static final String FAILED = "FAILED";
    /** Backend server has accepted shutdown request */
    public static final String SUCCESS = "SUCCESS";
    /** Backend server encountered an error */
    public static final String ERROR = "ERROR";
}
