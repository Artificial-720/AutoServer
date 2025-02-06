package me.artificial.autoserver.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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

    public static void runCommand(String path, String command) throws RuntimeException {
        ProcessBuilder processBuilder = getProcessBuilder(path, command);
//        processBuilder.inheritIO(); // this didn't work to see the error when directory is missing

        // maybe use the exit code for the process


        // Run the command
        try {
            System.out.println("Executing Command: " + String.join(" ", processBuilder.command()));
            Process process = processBuilder.start();

            System.out.println("Process started with PID: " + process.pid());
//            System.out.println("Process exited with code: " + process.waitFor());
//            process.waitFor(10, TimeUnit.SECONDS);
//            could use this to check if the process exits right away, probably error if it doesn't do anything right away
        } catch (NullPointerException e) {
            // if an element of the command list is null
            System.err.println("element of command is null");
            throw new RuntimeException(e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            // if the command is an empty list (has size 0)
            System.err.println("Command is empty");
            throw new RuntimeException(e.getMessage());
        } catch (UnsupportedOperationException e) {
            // If the operating system does not support the creation of processes
            System.err.println("Operating system does not support process creation");
            throw new RuntimeException(e.getMessage());
        } catch (SecurityException e) {
            // if a security manager exists and
            // its checkExec method doesn't allow creation of the subprocess
            System.err.println("Security manager has blocked the creation of the process");
            throw new RuntimeException(e.getMessage());
        } catch (IOException e) {
            System.err.println("IO error while executing the command: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
//        catch (InterruptedException e) {
//            System.err.println("Command execution was interrupted: " + e.getMessage());
//            Thread.currentThread().interrupt();
//        }

    }

    private static ProcessBuilder getProcessBuilder(String path, String command) {
        ProcessBuilder processBuilder;
        // Split on whitespace, ignoring whitespace inside quotes (single or double)
        List<String> tokenCommand = new ArrayList<>(List.of(command.split("\\s+(?=(?:[^'\"]*['\"][^'\"]*['\"])*[^'\"]*$)")));

        // Remove surrounding quotes
        ListIterator<String> iterator = tokenCommand.listIterator();
        while (iterator.hasNext()) {
            String word = iterator.next();
            iterator.set(word.replaceAll("^\"|\"$|^'|'$", ""));
        }

        processBuilder = new ProcessBuilder(tokenCommand);


        // setting the directory
        if (path != null) {
            processBuilder.directory(new File(path));
        }

        // redirect error stream to standard out of the main process
        processBuilder.redirectErrorStream(true);
        return processBuilder;
    }
}