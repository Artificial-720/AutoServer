package me.artificial.autoserver.common;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

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

    /**
     * Encodes a message and its signature into a binary format.
     * <p>
     * Format if security enabled:
     *  [4-byte total message length][4-byte message length][message][4-byte signature length][signature]
     * Format if security disabled:
     *  [4-byte total message length][4-byte message length][message]
     */
    public static byte[] encodeData(String command, Boolean securityEnabled, String secret) {
        if (securityEnabled) {
            assert secret != null;

            String signature;
            try {
                signature = HMAC.signMessage(command, secret);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            int totalLength = 4 + command.length() + 4 + signature.length();

            ByteBuffer buffer = ByteBuffer.allocate(4 + totalLength);
            buffer.putInt(totalLength);
            buffer.putInt(command.length());
            buffer.put(command.getBytes());
            buffer.putInt(signature.length());
            buffer.put(signature.getBytes());

            return buffer.array();
        }
        int totalLength = 4 + command.length();

        ByteBuffer buffer = ByteBuffer.allocate(4 + totalLength);
        buffer.putInt(totalLength);
        buffer.putInt(command.length());
        buffer.put(command.getBytes());

        return buffer.array();
    }

    public static DecodedMessage decodeData(byte[] data, boolean securityEnabled) {
        try {
            System.out.println("decode");
            ByteBuffer buffer = ByteBuffer.wrap(data);
            int commandLength = buffer.getInt();
            byte[] commandBytes = new byte[commandLength];
            buffer.get(commandBytes);
            String command = new String(commandBytes);

            String signature = null;
            if (securityEnabled) {
                int signatureLength = buffer.getInt();
                byte[] signatureBytes = new byte[signatureLength];
                buffer.get(signatureBytes);
                signature = new String(signatureBytes);
            }

            return new DecodedMessage((buffer.remaining() != 0), command, signature);
        } catch (BufferUnderflowException e) {
            return new DecodedMessage(true, null, null);
        }
    }

    public static class DecodedMessage {
        private final boolean malformed;
        private final String command;
        private final String signature;

        public DecodedMessage(boolean malformed, String command, String signature) {
            this.malformed = malformed;
            this.command = command;
            this.signature = signature;
        }

        public String getCommand() {
            return command;
        }

        public boolean isMalformed() {
            return malformed;
        }

        public boolean verify(String secret) {
            if (!malformed && signature == null) return true;
            boolean isValid = false;
            try {
                isValid = HMAC.verifyMessage(command, signature, secret);
            } catch (Exception ignored) {}
            return isValid;
        }

        @Override
        public String toString() {
            return "DecodedMessage{malformed=" + malformed + ", command=" + command + ", signature=" + signature + "}";
        }
    }
}
