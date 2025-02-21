package me.artificial.autoserver.velocity.startable;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.artificial.autoserver.common.HMAC;
import me.artificial.autoserver.velocity.AutoServer;
import me.artificial.autoserver.common.NetworkCommands;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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

            Optional<Boolean> securityEnabled = plugin.getConfig().getSecurity(server);
            String secret = plugin.getSecret();
            if (securityEnabled.isPresent() && securityEnabled.get()) {
                if (secret == null) {
                    plugin.getLogger().error("Security enabled for {} but no secret is present.", server.getServerInfo().getName());
                    throw new RuntimeException("Security failed.");
                }
            }

            // setup socket
            try (Socket socket = new Socket(ip, port.get());
                 InputStream input = socket.getInputStream();
                 OutputStream output = socket.getOutputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                 PrintWriter writer = new PrintWriter(output, true)) {

                // SEND boot command
                socket.setSoTimeout(TIMEOUT);
                plugin.getLogger().debug("Attempting to send BOOT command");
                String command = NetworkCommands.BOOT;
                writer.println(command);

                if (securityEnabled.isPresent() && securityEnabled.get()) {
                    plugin.getLogger().debug("Security enabled sending signature.");
                    // use security
                    String signature;
                    try {
                        signature = HMAC.signMessage(command, secret);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    writer.println(signature);
                }

                String response;
                while ((response = reader.readLine()) != null) {
                    plugin.getLogger().debug("Server Response: {}", response.trim());

                    // process response
                    BackendResponse backendResponse = new BackendResponse(response);
                    if (!backendResponse.isValid()) {
                        plugin.getLogger().warn(backendResponse.getErrorMsg());
                        continue;
                    }

                    switch (backendResponse.getStatus()) {
                        case NetworkCommands.ACKNOWLEDGED:
                            plugin.getLogger().info("Backend server has acknowledged boot command.");
                            break;
                        case NetworkCommands.COMPLETED:
                            plugin.getLogger().info("Backend server booting.");
                            return "Backend server booting";
                        case NetworkCommands.FAILED:
                            plugin.getLogger().error("Backend server failed to start. Message: {}", backendResponse.getMessage());
                            throw new RuntimeException("Backend server failed to start. Message: " + backendResponse.getMessage());
                        case NetworkCommands.ERROR:
                            plugin.getLogger().warn("Error occurred on the backend server with message: {}", backendResponse.getMessage());
                            break;
                        default: // Unrecognized status received from the backend
                            plugin.getLogger().warn("Unexpected status received: {}. Message: {}", backendResponse.getStatus(), backendResponse.getMessage());
                            break;
                    }

                    plugin.getLogger().trace("Command processed continuing loop.");
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

    private static class BackendResponse {
        private String status = null;
        private String message = null;
        private String errorMsg = null;

        public BackendResponse(String msg) {
            if (msg.isBlank()) {
                errorMsg = "Received an empty or null response.";
                return;
            }
            String[] parts = msg.split(":", 2);
            if (parts.length < 2) {
                errorMsg = "Malformed response received.";
                return;
            }
            status = parts[0].trim();
            message = parts[1].trim();
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public boolean isValid() {
            return errorMsg == null;
        }

        public String getErrorMsg() {
            return errorMsg;
        }
    }
}
