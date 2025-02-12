package me.artificial.autoserver.velocity.startable;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.artificial.autoserver.velocity.AutoServer;
import me.artificial.autoserver.velocity.NetworkCommands;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RemoteStartable implements Startable {
    private static final int TIMEOUT = 5000;
    private final AutoServer plugin;
    private final RegisteredServer server;

    public RemoteStartable(AutoServer plugin, RegisteredServer server) {
        this.plugin = plugin;
        this.server = server;
    }

    @Override
    public CompletableFuture<String> start() {
        return CompletableFuture.supplyAsync(() -> {
            InetAddress ip = server.getServerInfo().getAddress().getAddress();
            Optional<Integer> port = plugin.getConfig().getPort(server);
            if (port.isEmpty()) {
                plugin.getLogger().error("Invalid port value for server {}. Valid port range is 0 to 65535.", server.getServerInfo().getName());
                throw new RuntimeException("Invalid port value.");
            }

            // setup socket
            try (Socket socket = new Socket(ip, port.get());
                 InputStream input = socket.getInputStream();
                 OutputStream output = socket.getOutputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                 PrintWriter writer = new PrintWriter(output, true)) {

                socket.setSoTimeout(TIMEOUT);
                plugin.getLogger().debug("Attempting to send BOOT command");
                writer.println(NetworkCommands.BOOT);

                String response;
                while ((response = reader.readLine()) != null) {
                    plugin.getLogger().debug("Server Response: {}", response.trim());

                    // process response
                    if (response.isBlank()) {
                        plugin.getLogger().warn("Received an empty or null response.");
                        continue;
                    }
                    String[] parts = response.split(":", 2);
                    if (parts.length < 2) {
                        plugin.getLogger().warn("Malformed response received: {}", response);
                        continue;
                    }
                    String status = parts[0].trim();
                    String message = parts[1].trim();
                    plugin.getLogger().debug("Response Status: {}, Message: {}", status, message);

                    switch (status.toUpperCase()) {
                        case NetworkCommands.ACKNOWLEDGED: // Backend server received the boot command and has acknowledged it
                            plugin.getLogger().info("Backend server has acknowledged boot command.");
                            break;
                        case NetworkCommands.COMPLETED: // Backend server has executed the boot command successfully but is not yet running
                            plugin.getLogger().info("Backend server booting.");
                            return "Backend server booting";
                        case NetworkCommands.FAILED: // Backend server encountered an error during boot
                            plugin.getLogger().error("Backend server failed to start. Message: {}", message);
                            throw new RuntimeException("Backend server failed to start. Message: " + message);
                        case NetworkCommands.ERROR: // Backend server encountered an error
                            plugin.getLogger().warn("Error occurred on the backend server with message: {}", message);
                            break;
                        default: // Unrecognized status received from the backend
                            plugin.getLogger().warn("Unexpected status received: {}. Message: {}", status, message);
                            break;
                    }

                }
            } catch (IOException e) {
                throw new RuntimeException("Error while communicating with the server.");
            }
            throw new RuntimeException("Unknown Error.");
        });
    }

    @Override
    public CompletableFuture<String> stop() {
        return null;
    }
}
