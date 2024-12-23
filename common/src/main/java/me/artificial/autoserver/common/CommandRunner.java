package me.artificial.autoserver.common;

import java.io.IOException;

public class CommandRunner {
    public static boolean runCommand(String command) {
        String os = System.getProperty("os.name").toLowerCase();
        String path = "\"" + System.getProperty("user.dir").replace("\\", "/") + "\"";
        ProcessBuilder processBuilder;

        if (os.contains("win")) {
            System.out.println("Windows Detected");
            processBuilder = new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", "/D", path, "cmd", "/c", command);
        } else {
            System.out.println("Linux/Unix Detected");
            processBuilder = new ProcessBuilder("bash", "-c", String.format(
                    "nohup %s > output.log 2>&1 &",
                    command
            ));
        }

        try {
            System.out.println("Executing Command: " + String.join(" ", processBuilder.command()));
            Process process = processBuilder.start();

            System.out.println("Process started with PID: " + process.pid());
            System.out.println("Process exited with code: " + process.waitFor());
            return true;
        } catch (IOException e) {
            System.err.println("IO error while executing the command: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Command execution was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
        return false;
    }
}