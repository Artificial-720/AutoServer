package me.artificial.autoserver.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class CommandRunner {

    public static class CommandResult {
        private final boolean started;
        private final String errorMessage;
        private final String path;
        private final String command;
        private final Process process;

        public CommandResult(boolean started, String errorMessage, String path, String command, Process process) {
            this.started = started;
            this.errorMessage = errorMessage;
            this.path = path;
            this.command = command;
            this.process = process;
        }

        public boolean failedToStart() {
            return !started;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isTerminated() {
            return !process.isAlive();
        }

        public String getProcessOutput() {
            assert !process.isAlive() : "Must check that CommandResult.isTerminated() before calling getProcessOutput.";
            try {
                return new String(process.getInputStream().readAllBytes());
            } catch (IOException ignored) {}
            return "";
        }

        @Override
        public String toString() {
            return "CommandResult{" +
                    "started=" + started +
                    ", errorMessage='" + errorMessage + '\'' +
                    ", processId=" + process.pid() +
                    ", path=" + path +
                    ", command=" + command +
                    '}';
        }
    }

    public static CommandResult runCommand(String path, String command, Boolean preserveQuotes) {
        ProcessBuilder processBuilder = getProcessBuilder(path, command, preserveQuotes);

        boolean started = false;
        Process process = null;
        String errorMsg = null;
        String commandParsed = String.join(" ", processBuilder.command());

        // Run the command
        try {
            process = processBuilder.start();
            started = true;
        } catch (NullPointerException e) {
            // if an element of the command list is null
            errorMsg = "Element of command is null";
        } catch (IndexOutOfBoundsException e) {
            // if the command is an empty list (has size 0)
            errorMsg = "Command is empty";
        } catch (UnsupportedOperationException e) {
            // If the operating system does not support the creation of processes
            errorMsg = "Operating system does not support process creation";
        } catch (SecurityException e) {
            // if a security manager exists and
            // its checkExec method doesn't allow creation of the subprocess
            errorMsg = "Security manager has blocked the creation of the process";
        } catch (IOException e) {
            errorMsg = "IO error while executing the command: " + e.getMessage();
        }

        return new CommandResult(started, errorMsg, path, commandParsed, process);
    }

    private static ProcessBuilder getProcessBuilder(String path, String command, Boolean preserveQuotes) {
        ProcessBuilder processBuilder;
        String os = System.getProperty("os.name").toLowerCase();
        // Split on whitespace, ignoring whitespace inside quotes (single or double)
        List<String> tokenCommand = new ArrayList<>(List.of(command.split("\\s+(?=(?:[^'\"]*['\"][^'\"]*['\"])*[^'\"]*$)")));

        // Remove surrounding quotes if linux and keep if windows
        // new setting PRESERVE_QUOTES, that ignores the OS check
        if ((preserveQuotes == null && !os.contains("win")) || (preserveQuotes != null && !preserveQuotes)) {
            ListIterator<String> iterator = tokenCommand.listIterator();
            while (iterator.hasNext()) {
                String word = iterator.next();
                iterator.set(word.replaceAll("^\"|\"$|^'|'$", ""));
            }
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