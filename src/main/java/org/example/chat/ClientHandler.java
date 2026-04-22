package org.example.chat;

import org.example.chat.files.FileTransferManager;
import org.example.chat.files.FileTransferPeer;
import org.example.chat.handshake.HandshakeService;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameDecoder;
import org.example.chat.protocol.FrameEncoder;
import org.example.chat.protocol.FrameType;
import org.example.chat.security.EncryptionService;
import org.example.chat.util.Logger;
import org.example.chat.auth.UserSessionManager;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * ClientHandler represents a single connected client.
 *
 * Responsibilities:
 * - Own the client socket
 * - Perform handshake
 * - Read frames
 * - Route frames
 * - Send encrypted responses
 * - Manage client session state
 *
 * Concurrency:
 * - One thread per client
 * - sendLock prevents frame interleaving
 *
 * Cleanup:
 * - Idempotent
 * - Safe against double invocation
 */
public class ClientHandler implements Runnable, FileTransferPeer {

    private final Socket socket;
    private final ChatServer server;
    private final EncryptionService encryptionService;
    private final FrameRouter router;
    private final HandshakeService handshakeService;

    private InputStream rawIn;
    private OutputStream rawOut;

    private volatile boolean closed = false;
    private volatile boolean handshakeComplete = false;

    private String username;

    private final Object sendLock = new Object();

    private volatile long lastHeartbeat = System.currentTimeMillis();

    public ClientHandler(Socket socket,
                         ChatServer server,
                         EncryptionService encryptionService,
                         FrameRouter router,
                         HandshakeService handshakeService) {
        this.socket = socket;
        this.server = server;
        this.encryptionService = encryptionService;
        this.router = router;
        this.handshakeService = handshakeService;
    }

    /* =========================================================
     * Lifecycle
     * ========================================================= */

    @Override
    public void run() {
        try {
            rawIn = socket.getInputStream();
            rawOut = socket.getOutputStream();

            handshakeService.performHandshake(this);
            handshakeComplete = true;

            updateHeartbeat();

            Frame frame;
            while (!Thread.currentThread().isInterrupted()
                    && !socket.isClosed()
                    && (frame = readFrame()) != null) {

                try {
                    router.route(this, frame);
                } catch (Exception e) {
                    Logger.error("Frame handling failed", e);
                }
            }

        } catch (Exception e) {
            Logger.error("Client error", e);
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        if (closed) return;
        closed = true;

        try { socket.close(); } catch (Exception ignored) {}

        encryptionService.removeClient(this);
        server.removeClient(this);
        server.getFileTransferManager().abortTransfersForPeer(this);
    }

    /* =========================================================
     * Frame Utilities
     * ========================================================= */

    public Frame readFrame() throws Exception {
        return FrameDecoder.read(rawIn);
    }

    /* =========================================================
     * Messaging
     * ========================================================= */

    public void send(String message) {
        try {
            sendEncrypted(
                    FrameType.CHAT,
                    message.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            Logger.error("Send failed", e);
            cleanup();
        }
    }

    @Override
    public void sendEncrypted(FrameType type, byte[] payload) throws Exception {

        if (!handshakeComplete) {
            throw new IllegalStateException("Handshake not completed");
        }

        byte[] encrypted = encryptionService.encryptBytesForClient(
                this,
                type,
                payload
        );

        sendFrame(new Frame(type, encrypted));
    }

    public void sendFrame(Frame frame) {
        synchronized (sendLock) {
            try {
                FrameEncoder.write(frame, rawOut);
            } catch (Exception e) {
                Logger.error("Failed to send frame", e);
                cleanup();
            }
        }
    }

    /* =========================================================
     * Decryption Helpers
     * ========================================================= */

    public byte[] decrypt(Frame frame) throws Exception {
        return encryptionService.decryptBytesFromClient(
                this,
                frame.getType(),
                frame.getPayload()
        );
    }

    public String readUTF(Frame frame) throws Exception {
        try (var in = new java.io.DataInputStream(
                new java.io.ByteArrayInputStream(decrypt(frame)))) {
            return in.readUTF();
        }
    }

    public java.io.DataInputStream readStream(Frame frame) throws Exception {
        return new java.io.DataInputStream(
                new java.io.ByteArrayInputStream(decrypt(frame)));
    }

    /* =========================================================
     * Session
     * ========================================================= */

    public void setUsername(String username) {
        if (this.username != null) {
            throw new IllegalStateException("Username already set");
        }

        this.username = username;
        server.getSessionManager().registerSession(username, this);
    }

    public boolean isAuthenticated() {
        return username != null;
    }

    public String getUsername() {
        return username;
    }

    /* =========================================================
     * Server Access Helpers
     * ========================================================= */

    public FileTransferManager fileTransfers() {
        return server.getFileTransferManager();
    }

    public UserSessionManager sessions() {
        return server.getSessionManager();
    }

    public ChatServer getServer() {
        return server;
    }

    public Socket getSocket() {
        return socket;
    }

    public void updateHeartbeat() {
        lastHeartbeat = System.currentTimeMillis();
    }

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    /* =========================================================
     * Debug
     * ========================================================= */

    @Override
    public String toString() {
        return username != null
                ? "Client[" + username + "]"
                : "Client[" + socket.getRemoteSocketAddress() + "]";
    }
}
