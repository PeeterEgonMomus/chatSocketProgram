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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.example.chat.auth.UserSessionManager;

public class ClientHandler implements Runnable, FileTransferPeer {

    private final Socket socket;
    private final ChatServer server;
    private final EncryptionService encryptionService;
    private final FrameRouter router;

    private InputStream rawIn;
    private OutputStream rawOut;

    private String username;

    private final HandshakeService handshakeService;

    private final Object sendLock = new Object();

    public ClientHandler(Socket socket,
                         ChatServer server,
                         EncryptionService encryptionService,
                         FrameRouter router, HandshakeService handshakeService) {
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

            Frame frame;
            while ((frame = readFrame()) != null) {
                try {
                    router.route(this, frame);
                } catch (Exception e) {
                    Logger.error("Frame handling failed", e);
                }
            }

        } catch (Exception e) {
            Logger.error("Client error", e);
        } finally {
            try { socket.close(); } catch (Exception ignored) {}

            encryptionService.removeClient(this);
            server.removeClient(this);
            server.getFileTransferManager().abortTransfersForPeer(this);
        }
    }

    /* =========================================================
     * Frame Utilities
     * ========================================================= */

    public Frame readFrame() throws Exception {
        return FrameDecoder.read(rawIn);
    }

    /* =========================================================
     * Handshake
     * ========================================================= */



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
        }
    }

    @Override
    public void sendEncrypted(FrameType type, byte[] payload) throws Exception {

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

    public EncryptionService encryption() {
        return encryptionService;
    }

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

    /* =========================================================
     * Debug
     * ========================================================= */

    @Override
    public String toString() {
        return username != null ? username : socket.toString();
    }
}