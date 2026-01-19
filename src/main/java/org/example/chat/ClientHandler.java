package org.example.chat;

import org.example.chat.auth.UserSession;
import org.example.chat.files.FileTransferSession;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameDecoder;
import org.example.chat.protocol.FrameEncoder;
import org.example.chat.protocol.FrameType;
import org.example.chat.security.EncryptionService;
import org.example.chat.util.Logger;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChatServer server;
    private final EncryptionService encryptionService;

    private InputStream rawIn;
    private OutputStream rawOut;

    private boolean aesReady = false;
    private String username;
    private String clientPublicKey;

    // ✅ Single source of truth for file receiving
    private FileTransferSession fileSession;

    public ClientHandler(Socket socket, ChatServer server, EncryptionService encryptionService) {
        this.socket = socket;
        this.server = server;
        this.encryptionService = encryptionService;
    }

    @Override
    public void run() {
        try {
            rawIn = socket.getInputStream();
            rawOut = socket.getOutputStream();

            sendServerPublicKey();
            performHandshake();

            while (true) {
                Frame frame = FrameDecoder.read(rawIn);
                if (frame == null) break;
                handleFrame(frame);
            }

        } catch (IOException e) {
            Logger.error("Client disconnected", e);
        } catch (Exception e) {
            Logger.error("Client error", e);
        } finally {
            encryptionService.removeClient(this);
            server.removeClient(this);
        }
    }

    /* ================= HANDSHAKE ================= */

    private void sendServerPublicKey() throws IOException {
        String serverPub = encryptionService.getServerPublicKeyBase64();
        FrameEncoder.write(
                new Frame(FrameType.HANDSHAKE_SERVER_KEY, serverPub.getBytes(StandardCharsets.UTF_8)),
                rawOut
        );
    }

    private void performHandshake() throws Exception {
        while (!aesReady) {
            Frame frame = FrameDecoder.read(rawIn);
            if (frame == null) throw new IOException("Client disconnected during handshake");

            switch (frame.getType()) {
                case HANDSHAKE_CLIENT_KEY -> {
                    clientPublicKey = new String(frame.getPayload(), StandardCharsets.UTF_8);
                }
                case HANDSHAKE_AES_KEY -> {
                    String encryptedAES = new String(frame.getPayload(), StandardCharsets.UTF_8);
                    encryptionService.registerClientAESKey(this, encryptedAES);
                    FrameEncoder.write(new Frame(FrameType.HANDSHAKE_OK, new byte[0]), rawOut);
                    aesReady = true;
                }
                default -> throw new IllegalStateException("Unexpected handshake frame: " + frame.getType());
            }
        }
    }

    /* ================= FRAME ROUTING ================= */

    private void handleFrame(Frame frame) {
        try {
            switch (frame.getType()) {

                case CHAT -> handleChat(frame);

                case FILE_META -> handleFileMeta(frame);

                case FILE_CHUNK -> handleFileChunk(frame);

                case FILE_END -> handleFileEnd();

                case FILE_ACCEPT -> handleFileAccept(
                        new String(frame.getPayload(), StandardCharsets.UTF_8)
                );

                case FILE_REJECT -> handleFileReject(
                        new String(frame.getPayload(), StandardCharsets.UTF_8)
                );

                default -> Logger.debug("Unhandled frame type: " + frame.getType());
            }
        } catch (Exception e) {
            Logger.error("Failed to handle frame " + frame.getType(), e);
            abortFileTransfer();
        }
    }

    private void handleChat(Frame frame) throws Exception {
        String encrypted = new String(frame.getPayload(), StandardCharsets.UTF_8);
        String message = encryptionService.decryptFromClient(this, encrypted);
        server.getRegistry().executeCommand(this, message);
    }

    /* ================= FILE TRANSFER ================= */

    private void handleFileMeta(Frame frame) throws Exception {
        String meta = new String(frame.getPayload(), StandardCharsets.UTF_8);
        String[] parts = meta.split("\\|");

        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid FILE_META format");
        }

        String recipient = parts[0];
        String filename = parts[1];
        long size = Long.parseLong(parts[2]);
        String checksum = parts[3];

        fileSession = new FileTransferSession();
        fileSession.start(recipient, filename, size, checksum);

        Logger.info("Started FILE_META for '" + filename + "' (" + size + " bytes)");
    }

    private void handleFileChunk(Frame frame) throws Exception {
        if (fileSession == null) {
            throw new IllegalStateException("FILE_CHUNK without FILE_META");
        }

        byte[] decrypted = encryptionService.decryptBytesFromClient(this, frame.getPayload());

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(decrypted));
        int index = in.readInt();
        int length = in.readInt();

        byte[] data = in.readNBytes(length);

        Logger.debug("SERVER CHUNK #" + index + " | bytes=" + data.length);

        fileSession.acceptChunk(index, data);
    }

    private void handleFileEnd() throws Exception {
        if (fileSession == null) {
            throw new IllegalStateException("FILE_END without active transfer");
        }

        fileSession.finish();
        forwardFileToRecipient(fileSession);
        fileSession = null;
    }

    private void abortFileTransfer() {
        if (fileSession != null) {
            fileSession.abort();
            fileSession = null;
        }
    }

    /* ================= FILE DELIVERY ================= */

    private void forwardFileToRecipient(FileTransferSession session) throws IOException {
        var targetSession = server.getSessionManager()
                .getSessionByUsername(session.getRecipient())
                .map(UserSession::getChatHandler)
                .orElse(null);

        if (targetSession == null) {
            send("User '" + session.getRecipient() + "' is offline. File stored.");
            return;
        }

        PendingFile pending = new PendingFile(
                this,
                targetSession,
                session.getFile(),
                session.getSize(),
                null
        );

        server.addPendingFile(session.getFilename(), pending);

        targetSession.send(
                "User '" + this + "' wants to send you file '" +
                        session.getFilename() + "'. Type /accept " + session.getFilename()
        );

        send("File uploaded. Waiting for recipient.");
    }

    /* ================= FILE ACCEPT / REJECT ================= */

    private void handleFileAccept(String filename) {
        PendingFile pending = server.getPendingFile(filename);

        if (pending == null || pending.getRecipient() != this) {
            send("No pending file named '" + filename + "'");
            return;
        }

        try {
            pending.sendToRecipient();
            server.removePendingFile(filename);
            pending.getSender().send("Your file '" + filename + "' was accepted.");
            send("You received the file '" + filename + "'.");
            pending.cleanup();
        } catch (Exception e) {
            Logger.error("File delivery failed", e);
        }
    }

    private void handleFileReject(String filename) {
        PendingFile pending = server.getPendingFile(filename);

        if (pending == null || pending.getRecipient() != this) {
            send("No pending file named '" + filename + "'");
            return;
        }

        pending.getSender().send("Your file '" + filename + "' was rejected.");
        pending.cleanup();
        server.removePendingFile(filename);
    }

    /* ================= OUTBOUND ================= */

    public void sendChat(String message) {
        try {
            String encrypted = encryptionService.encryptForClient(this, message, clientPublicKey);
            FrameEncoder.write(
                    new Frame(FrameType.CHAT, encrypted.getBytes(StandardCharsets.UTF_8)),
                    rawOut
            );
        } catch (Exception e) {
            Logger.error("Failed to send message", e);
        }
    }

    public void send(String message) {
        sendChat(message);
    }

    /* ================= ACCESSORS ================= */

    public void setUsername(String username) {
        this.username = username;
        server.getSessionManager().registerSession(username, this);
    }

    public boolean isAuthenticated() {
        return username != null;
    }

    @Override
    public String toString() {
        return username != null
                ? username
                : socket.getRemoteSocketAddress().toString();
    }

    public Socket getSocket() {
        return socket;
    }

    public void broadcast(String message) {
        server.broadcast(this, message);
    }


}
