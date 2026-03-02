package org.example.chat;

import org.example.chat.files.*;
import org.example.chat.files.FileDescriptor;
import org.example.chat.protocol.*;
import org.example.chat.security.EncryptionService;
import org.example.chat.util.Logger;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable, FileTransferPeer {

    private final Socket socket;
    private final ChatServer server;
    private final EncryptionService encryptionService;

    private InputStream rawIn;
    private OutputStream rawOut;

    private boolean aesReady = false;
    private String username;

    private final Object sendLock = new Object();

    public ClientHandler(Socket socket,
                         ChatServer server,
                         EncryptionService encryptionService) {
        this.socket = socket;
        this.server = server;
        this.encryptionService = encryptionService;
    }

    /* =========================================================
     * Lifecycle
     * ========================================================= */

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

        } catch (Exception e) {
            Logger.error("Client error", e);
        } finally {
            encryptionService.removeClient(this);
            server.removeClient(this);
            server.getFileTransferManager()
                    .abortTransfersForPeer(this);
        }
    }

    /* =========================================================
     * Handshake
     * ========================================================= */

    private void sendServerPublicKey() throws IOException {
        FrameEncoder.write(
                new Frame(
                        FrameType.HANDSHAKE_SERVER_KEY,
                        encryptionService.getServerPublicKeyBase64()
                                .getBytes(StandardCharsets.UTF_8)
                ),
                rawOut
        );
    }

    private void performHandshake() throws Exception {

        while (!aesReady) {

            Frame frame = FrameDecoder.read(rawIn);
            if (frame == null)
                throw new IOException("Handshake aborted");

            switch (frame.getType()) {

                case FILE_START -> handleFileStart(frame);

                case HANDSHAKE_CLIENT_KEY ->
                        Logger.debug("Client public key received (ignored)");

                case HANDSHAKE_AES_KEY -> {

                    encryptionService.registerClientAESKey(
                            this,
                            new String(frame.getPayload(), StandardCharsets.UTF_8)
                    );

                    FrameEncoder.write(
                            new Frame(FrameType.HANDSHAKE_OK, new byte[0]),
                            rawOut
                    );

                    aesReady = true;
                    Logger.info("Handshake completed for client " + this);
                }

                default -> throw new IllegalStateException("Invalid handshake frame");
            }
        }
    }

    /* =========================================================
     * Frame Routing
     * ========================================================= */

    private void handleFrame(Frame frame) {
        try {

            switch (frame.getType()) {

                case CHAT -> handleChat(frame);

                case SEND_FILE_REQUEST -> handleSendFileRequest(frame);

                case FILE_OFFER -> handleFileOffer(frame);

                case FILE_START -> handleFileStart(frame);

                case FILE_CHUNK -> handleFileChunk(frame);

                case FILE_END -> handleFileEnd(frame);

                case FILE_ACCEPT -> handleFileAccept(readUTF(frame));

                case FILE_REJECT -> handleFileReject(readUTF(frame));
            }

        } catch (Exception e) {
            Logger.error("Frame handling failed", e);
        }
    }

    /* =========================================================
     * Decryption Helpers (NEW)
     * ========================================================= */

    private byte[] decrypt(Frame frame) throws Exception {
        return encryptionService.decryptBytesFromClient(
                this,
                frame.getType(),
                frame.getPayload()
        );
    }

    private String readUTF(Frame frame) throws Exception {
        try (DataInputStream in =
                     new DataInputStream(new ByteArrayInputStream(decrypt(frame)))) {
            return in.readUTF();
        }
    }

    private DataInputStream readStream(Frame frame) throws Exception {
        return new DataInputStream(
                new ByteArrayInputStream(decrypt(frame))
        );
    }

    /* =========================================================
     * Chat
     * ========================================================= */

    private void handleChat(Frame frame) throws Exception {

        byte[] decrypted = decrypt(frame);

        server.getRegistry().executeCommand(
                this,
                new String(decrypted, StandardCharsets.UTF_8)
        );
    }

    /* =========================================================
     * File Negotiation
     * ========================================================= */

    private void handleFileOffer(Frame frame) throws Exception {

        DataInputStream in = readStream(frame);

        String transferId = in.readUTF();
        String filename = in.readUTF();
        long size = in.readLong();
        String checksum = in.readUTF();

        ActiveFileTransfer transfer =
                server.getFileTransferManager().getById(transferId);

        if (transfer == null)
            throw new IllegalStateException("Unknown transfer " + transferId);

        if (transfer.getSender() != this)
            throw new IllegalStateException("Sender mismatch " + transferId);

        if (transfer.getState() != ActiveFileTransfer.State.WAITING_FOR_RECIPIENT)
            throw new IllegalStateException(
                    "Transfer " + transferId + " not waiting for recipient"
            );

        transfer.registerDescriptor(
                new FileDescriptor(
                        transferId,
                        filename,
                        size,
                        checksum
                )
        );

        ClientHandler recipient =
                (ClientHandler) transfer.getRecipient();

        forwardOffer(recipient,
                transferId,
                filename,
                size,
                checksum);
    }

    private void handleFileStart(Frame frame) throws Exception {

        DataInputStream in = readStream(frame);

        String transferId = in.readUTF();

        ActiveFileTransfer transfer =
                server.getFileTransferManager().getById(transferId);

        if (transfer == null)
            throw new IllegalStateException("Unknown transfer " + transferId);

        if (transfer.getState() != ActiveFileTransfer.State.UPLOADING)
            throw new IllegalStateException("Transfer not ready for FILE_START");

        transfer.startUploadSession();
    }

    private void forwardOffer(
            ClientHandler recipient,
            String transferId,
            String filename,
            long size,
            String checksum
    ) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeUTF(transferId);
        out.writeUTF(this.username);
        out.writeUTF(filename);
        out.writeLong(size);
        out.writeUTF(checksum);

        byte[] encrypted =
                encryptionService.encryptBytesForClient(
                        recipient,
                        FrameType.FILE_OFFER,
                        baos.toByteArray()
                );

        recipient.sendFrame(new Frame(FrameType.FILE_OFFER, encrypted));
    }

    /* =========================================================
     * Upload Pipeline
     * ========================================================= */

    private void handleFileChunk(Frame frame) throws Exception {

        DataInputStream in = readStream(frame);

        String transferId = in.readUTF();
        int index = in.readInt();
        int len = in.readInt();

        byte[] data = in.readNBytes(len);

        ActiveFileTransfer transfer =
                server.getFileTransferManager().getById(transferId);

        if (transfer == null)
            throw new IllegalStateException("Unknown transfer " + transferId);

        if (transfer.getState() != ActiveFileTransfer.State.UPLOADING)
            throw new IllegalStateException(
                    "Transfer " + transferId + " not ready for upload"
            );

        transfer.acceptChunk(index, data);
    }

    private void handleFileEnd(Frame frame) throws Exception {

        String transferId = readUTF(frame);

        ActiveFileTransfer transfer =
                server.getFileTransferManager().getById(transferId);

        if (transfer == null)
            throw new IllegalStateException("Unknown transfer " + transferId);

        if (transfer.getState() != ActiveFileTransfer.State.UPLOADING)
            throw new IllegalStateException("Transfer " + transferId + " not uploading");

        transfer.finishUpload();
        transfer.accept();
    }

    /* =========================================================
     * Accept / Reject
     * ========================================================= */

    private void handleFileAccept(String transferId) throws Exception {

        ActiveFileTransfer transfer =
                server.getFileTransferManager().getById(transferId);

        if (transfer == null || transfer.getRecipient() != this) {

            sendEncrypted(
                    FrameType.ERROR,
                    ("Invalid file id: " + transferId)
                            .getBytes(StandardCharsets.UTF_8)
            );
            return;
        }

        transfer.setState(ActiveFileTransfer.State.UPLOADING);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeUTF(transferId);

        transfer.getSender().sendEncrypted(
                FrameType.SEND_FILE_READY,
                baos.toByteArray()
        );
    }

    private void handleFileReject(String transferId) throws Exception {

        ActiveFileTransfer transfer =
                server.getFileTransferManager().getById(transferId);

        if (transfer == null || transfer.getRecipient() != this)
            return;

        transfer.setState(ActiveFileTransfer.State.REJECTED);

        transfer.getSender().sendEncrypted(
                FrameType.ERROR,
                ("Recipient rejected the file: " + transferId)
                        .getBytes(StandardCharsets.UTF_8)
        );

        server.getFileTransferManager().remove(transferId);
    }

    /* =========================================================
     * SEND_FILE_REQUEST
     * ========================================================= */

    private void handleSendFileRequest(Frame frame) throws Exception {

        DataInputStream in = readStream(frame);

        String transferId = in.readUTF();
        String recipientName = in.readUTF();

        if (!isAuthenticated()) {
            send("You must be authenticated to send files.");
            return;
        }

        var sessionOpt =
                server.getSessionManager()
                        .getSessionByUsername(recipientName);

        if (sessionOpt.isEmpty()) {
            send("User not online.");
            return;
        }

        ClientHandler recipient =
                sessionOpt.get().getChatHandler();

        if (recipient == this) {
            send("You cannot send a file to yourself.");
            return;
        }

        ActiveFileTransfer transfer =
                server.getFileTransferManager()
                        .createTransfer(
                                transferId,
                                this,
                                recipient
                        );

        transfer.setState(
                ActiveFileTransfer.State.WAITING_FOR_RECIPIENT
        );
    }

    /* =========================================================
     * Utilities
     * ========================================================= */

    public void send(String message) {
        try {
            byte[] encrypted =
                    encryptionService.encryptBytesForClient(
                            this,
                            FrameType.CHAT,
                            message.getBytes(StandardCharsets.UTF_8)
                    );
            sendFrame(new Frame(FrameType.CHAT, encrypted));
        } catch (Exception e) {
            Logger.error("Send failed", e);
        }
    }

    @Override
    public void sendEncrypted(FrameType type, byte[] payload) throws Exception {
        byte[] encrypted =
                encryptionService.encryptBytesForClient(
                        this,
                        type,
                        payload
                );
        sendFrame(new Frame(type, encrypted));
    }

    public void setUsername(String username) {
        this.username = username;
        server.getSessionManager().registerSession(username, this);
    }

    public boolean isAuthenticated() {
        return username != null;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public ChatServer getServer() {
        return server;
    }

    @Override
    public String toString() {
        return username != null ? username : socket.toString();
    }

    private void sendFrame(Frame frame) throws IOException {
        synchronized (sendLock) {
            FrameEncoder.write(frame, rawOut);
        }
    }
}