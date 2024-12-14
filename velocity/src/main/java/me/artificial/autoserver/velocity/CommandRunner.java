package me.artificial.autoserver.velocity;

import java.io.IOException;

public class CommandRunner {
    public static boolean runCommand(String command) {
        String os = System.getProperty("os.name").toLowerCase();
        String path = "\"" + System.getProperty("user.dir").replace("\\", "/") + "\"";
        ProcessBuilder processBuilder;

        if (os.contains("win")) {
            System.out.println("Windows Detected");
            //processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            processBuilder = new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", "/D", path, "cmd", "/c", command);
        } else {
            System.out.println("Linux/Unix Detected");
            processBuilder = new ProcessBuilder("bash", "-c", command);
        }

        try {
            System.out.println("Executing Command: " + String.join(" ", processBuilder.command()));
            Process process = processBuilder.start();
            System.out.println("Process started with id: " + process.pid());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
