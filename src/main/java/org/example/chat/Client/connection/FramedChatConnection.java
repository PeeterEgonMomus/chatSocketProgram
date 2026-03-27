package org.example.chat.Client.connection;

import org.example.chat.Client.protocol.ClientCipher;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameDecoder;
import org.example.chat.protocol.FrameEncoder;
import org.example.chat.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Concrete TCP Frame Transport Implementation.
 *
 * Responsibility:
 * - Wraps a Socket
 * - Encodes/decodes protocol frames
 * - Applies optional encryption
 * - Ensures thread-safe sending
 *
 * Architecture Role:
 * This is the concrete implementation of FramedConnection.
 *
 * It forms the boundary between:
 *      • Network I/O
 *      • Client protocol layer
 *
 * Encryption Model:
 * - Starts in plaintext mode (for handshake)
 * - Cipher is installed later (post-handshake)
 * - All subsequent payloads are encrypted/decrypted automatically
 *
 * Thread Safety:
 * - Sending is synchronized using sendLock
 * - Prevents frame interleaving across threads
 *
 * Logging:
 * - Logs all inbound and outbound frame metadata
 * - Enables protocol-level tracing
 *
 * This class does NOT:
 * - Perform handshake logic
 * - Manage session keys
 * - Interpret frame meaning
 *
 * It strictly handles transport + optional crypto.
 */
public final class FramedChatConnection implements FramedConnection, AutoCloseable {

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;

    /**
     * Installed after handshake completes.
     * When null → plaintext mode.
     */
    private ClientCipher cipher;

    /**
     * Ensures atomic frame writes.
     */
    private final Object sendLock = new Object();

    public FramedChatConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
    }

    /**
     * Installs session cipher.
     *
     * Called after AES key negotiation.
     */
    public void setCipher(ClientCipher cipher) {
        this.cipher = cipher;
    }

    /**
     * Sends a frame.
     *
     * Flow:
     * 1. Encrypt payload (if cipher active)
     * 2. Rebuild frame with encrypted payload
     * 3. Encode frame
     * 4. Write to socket
     *
     * Entire operation is synchronized
     * to prevent frame corruption.
     */
    public void send(Frame frame) throws Exception {

        synchronized (sendLock) {

            byte[] payload = frame.getPayload();

            if (cipher != null) {
                payload = cipher.encrypt(frame.getType(), payload);
            }

            Frame encryptedFrame = new Frame(frame.getType(), payload);

            Logger.debug("[FRAME →] type=" + frame.getType() +
                    ", length=" + payload.length);

            FrameEncoder.write(encryptedFrame, out);
        }
    }

    /**
     * Receives and optionally decrypts a frame.
     *
     * Flow:
     * 1. Decode raw frame from stream
     * 2. Log metadata
     * 3. Decrypt payload if cipher active
     * 4. Return new Frame instance
     *
     * Returns null on clean socket close.
     */
    public Frame receive() throws Exception {
        Frame frame = FrameDecoder.read(in);
        if (frame == null) return null;

        Logger.debug("[FRAME ←] type=" + frame.getType() +
                ", length=" + frame.getPayload().length);

        byte[] payload = frame.getPayload();

        if (cipher != null) {
            try {
                payload = cipher.decrypt(frame.getType(), payload);
            } catch (Exception e) {
                Logger.error("Failed to decrypt frame of type " +
                        frame.getType() +
                        " | payload length=" + payload.length, e);
                throw e;
            }
        }

        return new Frame(frame.getType(), payload);
    }

    /**
     * Closes underlying socket.
     */
    @Override
    public void close() throws Exception {
        socket.close();
    }
}