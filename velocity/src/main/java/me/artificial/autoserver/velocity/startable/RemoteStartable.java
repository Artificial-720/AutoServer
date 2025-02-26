package me.artificial.autoserver.velocity.startable;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.artificial.autoserver.velocity.AutoServer;
import me.artificial.autoserver.common.NetworkCommands;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
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
            plugin.getLogger().trace("RemoteStartable start enter");
            // validate port
            InetAddress ip = server.getServerInfo().getAddress().getAddress();
            Optional<Integer> port = plugin.getConfig().getPort(server);
            if (port.isEmpty()) {
                plugin.getLogger().error("Invalid port value for server {}. Valid port range is 0 to 65535.", server.getServerInfo().getName());
                throw new RuntimeException("Invalid port value.");
            }
            plugin.getLogger().trace("Port is valid");

            boolean securityEnabled = plugin.getConfig().getSecurity(server);
            String secret = plugin.getSecret();
            if (securityEnabled && secret == null) {
                plugin.getLogger().error("Security enabled for {} but no secret is present.", server.getServerInfo().getName());
                throw new RuntimeException("Security failed.");
            }

            // setup socket
            try (Socket socket = new Socket(ip, port.get());
                 InputStream input = socket.getInputStream();
                 OutputStream output = socket.getOutputStream()) {
                socket.setSoTimeout(TIMEOUT);

                plugin.getLogger().debug("Attempting to send BOOT command");

                // check if legacy mode enabled
                int communicationVersion = plugin.getConfig().getCommunicationVersion(server);
                plugin.getLogger().trace("communicationVersion is this {}", communicationVersion);
                if (communicationVersion == 1) {
                    // 1.2.1 version of communication
                    // Start of section that will be deprecated---------------------------------------------------------
                    plugin.getLogger().warn("WARNING: Legacy communication protocol (< v1.4.0) is currently in use.");
                    plugin.getLogger().warn("This legacy mode is a temporary fallback and will be deprecated in a future release.");
                    plugin.getLogger().warn("For uninterrupted remote compatibility and access to new features, please update your backend plugin/mod to version 1.4.0 or later.");
                    String commandBoot = "BOOT_SERVER\n";
                    output.write(commandBoot.getBytes());
                    output.flush();

                    byte[] buffer = new byte[1024];
                    int read;
                    boolean noResponse = true;
                    while ((read = input.read(buffer)) != -1) {
                        String response = new String(buffer, 0, read);
                        noResponse = false;
                        plugin.getLogger().debug("Server Response: {}", response.trim());

                        if (response.isBlank()) {
                            plugin.getLogger().debug("Received an empty or null response.");
                            return "";
                        }

                        String[] parts = response.split(":", 2);
                        if (parts.length < 2) {
                            plugin.getLogger().debug("Malformed response received: {}", response);
                            return "";
                        }

                        String status = parts[0].trim();
                        String message = parts[1].trim();
                        plugin.getLogger().debug("Response Status: {}, Message: {}", status, message);

                        switch (status.toUpperCase()) {
                            case "ACKNOWLEDGED":
                                plugin.getLogger().info("Backend server has acknowledged boot command.");
                                break;
                            case "COMPLETED":
                                plugin.getLogger().info("Backend server booting.");
                                return "Backend server booting";
                            case "FAILED":
                                plugin.getLogger().error("Backend server failed to start. Message: {}", message);
                                throw new RuntimeException("Backend server failed to start.");
                            case "ERROR":
                                plugin.getLogger().warn("Error occurred on the backend server with message: {}", message);
                                break;
                            default:
                                plugin.getLogger().warn("Unexpected status received: {}. Message: {}", status, message);
                                break;
                        }
                    }
                    if (noResponse) {
                        plugin.getLogger().warn("No response received from the server.");
                        throw new RuntimeException("No response received from the server.");
                    }
                    // End of section that will be deprecated-----------------------------------------------------------
                } else {
                    // 1.4.0 version of communication
                    byte[] encoded = NetworkCommands.encodeData(NetworkCommands.BOOT, securityEnabled, secret);
                    output.write(encoded);
                    output.flush();

                    while (true) {
                        byte[] lengthBytes = new byte[4];
                        if (input.read(lengthBytes) != 4) break;

                        ByteBuffer lengthBuffer = ByteBuffer.wrap(lengthBytes);
                        int totalLength = lengthBuffer.getInt();

                        byte[] dataBytes = new byte[totalLength];
                        if (input.read(dataBytes) != totalLength) break;

                        NetworkCommands.DecodedMessage decodedMessage = NetworkCommands.decodeData(dataBytes, securityEnabled);

                        // Handle command
                        plugin.getLogger().debug("Received command: {}", decodedMessage);
                        switch (decodedMessage.getCommand()) {
                            case NetworkCommands.ACKNOWLEDGED:
                                plugin.getLogger().info("Backend server has acknowledged boot command.");
                                break;
                            case NetworkCommands.COMPLETED:
                                plugin.getLogger().info("Backend server booting.");
                                return "Backend server booting";
                            case NetworkCommands.FAILED:
                                plugin.getLogger().error("Backend server failed to start.");
                                throw new RuntimeException("Backend server failed to start.");
                            case NetworkCommands.ERROR:
                                plugin.getLogger().warn("Error occurred on the backend server.");
                                break;
                            default: // Unrecognized status received from the backend
                                plugin.getLogger().warn("Unexpected command: {}", decodedMessage.getCommand());
                                break;
                        }
                    }
                }
            } catch (SocketTimeoutException e) {
                plugin.getLogger().error("Timeout waiting for server response.");
            } catch (SocketException e) {
                plugin.getLogger().error("Socket closed, exiting read loop.");
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
