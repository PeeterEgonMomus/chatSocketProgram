package org.example.chat;

import org.example.chat.auth.UserSession;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameDecoder;
import org.example.chat.protocol.FrameEncoder;
import org.example.chat.protocol.FrameType;
import org.example.chat.security.EncryptionService;
import org.example.chat.util.Logger;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {

    private static final int MAX_FILE_SIZE = 100_000_000; // 100 MB safeguard

    private FileOutputStream currentFileOut;
    private String currentFileName;
    private long bytesReceived;

    private String currentFileRecipient;


    private final Socket socket;
    private final ChatServer server;
    private final EncryptionService encryptionService;

    private OutputStream rawOut;
    private InputStream rawIn;

    private boolean aesReady = false;
    private String username;
    private String clientPublicKey;

    private MessageDigest currentFileDigest;
    private String currentFileExpectedChecksum;


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

            BufferedReader textIn =
                    new BufferedReader(new InputStreamReader(rawIn, StandardCharsets.UTF_8));
            PrintWriter textOut =
                    new PrintWriter(new OutputStreamWriter(rawOut, StandardCharsets.UTF_8), true);

            /* ================= HANDSHAKE (PLAINTEXT) ================= */

            String serverPub = server.getEncryptionService().getServerPublicKeyBase64();
            textOut.println("PUBLIC_KEY:" + serverPub);
            Logger.debug("Sent PUBLIC_KEY to " + socket.getRemoteSocketAddress());

            String line;
            while (!aesReady && (line = textIn.readLine()) != null) {

                if (line.startsWith("CLIENT_KEY:")) {
                    clientPublicKey = line.substring("CLIENT_KEY:".length()).trim();
                    Logger.debug("Registered client public key");
                    continue;
                }

                if (line.startsWith("AES_KEY:")) {
                    String encryptedAES = line.substring("AES_KEY:".length()).trim();
                    encryptionService.registerClientAESKey(this, encryptedAES);
                    textOut.println("AES_OK");
                    aesReady = true;
                    Logger.debug("AES handshake completed");
                }
            }

            /* ================= FRAME LOOP (AES) ================= */

            while (true) {
                Frame frame = FrameDecoder.read(rawIn);
                if (frame == null) break;

                handleFrame(frame);
            }

        } catch (IOException e) {
            Logger.error("Client disconnected", e);
        } finally {
            encryptionService.removeClient(this);
            server.removeClient(this);
        }
    }

    /* ================= FRAME ROUTING ================= */

    private void handleFrame(Frame frame) {
        try {
            switch (frame.getType()) {

                /* ================= CHAT ================= */

                case CHAT -> {
                    String encrypted = new String(frame.getPayload(), StandardCharsets.UTF_8);
                    String message = encryptionService.decryptFromClient(this, encrypted);
                    server.getRegistry().executeCommand(this, message);
                }

                /* ================= FILE TRANSFER ================= */

                case FILE_META -> {
                    String meta = new String(frame.getPayload(), StandardCharsets.UTF_8);

                    // recipient|filename|size|checksum
                    String[] parts = meta.split("\\|");
                    if (parts.length != 4) {
                        Logger.error("Invalid FILE_META format");
                        return;
                    }

                    currentFileRecipient = parts[0];
                    currentFileName = parts[1];
                    long size;
                    try {
                        size = Long.parseLong(parts[2]);
                    } catch (NumberFormatException e) {
                        Logger.error("Invalid file size in FILE_META");
                        return;
                    }
                    currentFileExpectedChecksum = parts[3];

                    if (size <= 0 || size > MAX_FILE_SIZE) {
                        Logger.error("Rejected file: invalid size " + size);
                        return;
                    }

                    File file = new File("received_" + currentFileName);
                    currentFileOut = new FileOutputStream(file);
                    bytesReceived = 0;

                    // Prepare SHA-256 digest for incremental update
                    currentFileDigest = MessageDigest.getInstance("SHA-256");

                    Logger.info("Receiving file '" + currentFileName + "' (" + size + " bytes) from '" + this + "' for recipient '" + currentFileRecipient + "'");
                }


                case FILE_CHUNK -> {
                    if (currentFileOut == null) {
                        Logger.error("Received FILE_CHUNK without FILE_META");
                        return;
                    }

                    currentFileOut.write(frame.getPayload());
                    bytesReceived += frame.getPayload().length;

                    // Incrementally update SHA-256 digest
                    currentFileDigest.update(frame.getPayload());

                    Logger.debug("Received file chunk (" + frame.getPayload().length + " bytes)");
                }


                case FILE_END -> {
                    if (currentFileOut == null) {
                        Logger.error("Received FILE_END without active file transfer");
                        return;
                    }

                    currentFileOut.close();

                    // Compute final checksum
                    String receivedChecksum = Base64.getEncoder().encodeToString(currentFileDigest.digest());
                    if (!receivedChecksum.equals(currentFileExpectedChecksum)) {
                        Logger.error("Checksum mismatch for file " + currentFileName);
                        send("File transfer failed: checksum mismatch for " + currentFileName);
                        new File("received_" + currentFileName).delete();
                        currentFileOut = null;
                        currentFileName = null;
                        currentFileRecipient = null;
                        bytesReceived = 0;
                        return;
                    }

                    Logger.info("File upload completed and checksum verified: " + currentFileName);

                    // Continue normal PendingFile flow
                    var optionalSession = server.getSessionManager().getSessionByUsername(currentFileRecipient);
                    ClientHandler recipient = optionalSession.map(UserSession::getChatHandler).orElse(null);

                    if (recipient == null) {
                        send("User '" + currentFileRecipient + "' is not online. File stored but not delivered.");
                        Logger.debug("Recipient not online: " + currentFileRecipient);
                    } else {
                        // ✅ Create PendingFile with checksum
                        PendingFile pending = new PendingFile(
                                this,
                                recipient,
                                new File("received_" + currentFileName),
                                bytesReceived,
                                currentFileExpectedChecksum
                        );
                        server.addPendingFile(currentFileName, pending);
                        recipient.send("User '" + this + "' wants to send you file '" + currentFileName + "' (" + bytesReceived + " bytes). Type: /accept " + currentFileName);
                        send("File uploaded. Waiting for '" + currentFileRecipient + "' to accept.");
                    }

                    // Reset state
                    currentFileOut = null;
                    currentFileName = null;
                    currentFileRecipient = null;
                    bytesReceived = 0;
                    currentFileDigest = null;
                    currentFileExpectedChecksum = null;
                }



                case FILE_ACCEPT -> {
                    String filename =
                            new String(frame.getPayload(), StandardCharsets.UTF_8);
                    handleFileAccept(filename);
                }

                case FILE_REJECT -> {
                    String filename = new String(frame.getPayload(), StandardCharsets.UTF_8);
                    handleFileReject(filename);
                }




                default -> Logger.debug("Unhandled frame type: " + frame.getType());
            }
        } catch (Exception e) {
            Logger.error("Failed to handle frame " + frame.getType(), e);
        }
    }


    /* ================= OUTBOUND ================= */

    public void sendChat(String message) {
        try {
            String encrypted =
                    encryptionService.encryptForClient(this, message, clientPublicKey);

            Frame frame = new Frame(
                    FrameType.CHAT,
                    encrypted.getBytes(StandardCharsets.UTF_8)
            );

            FrameEncoder.write(frame, rawOut);
        } catch (Exception e) {
            Logger.error("Failed to send chat frame", e);
        }
    }

    /* ================= ACCESSORS ================= */

    public Socket getSocket() {
        return socket;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        server.getSessionManager().registerSession(username, this);
    }

    public String getClientPublicKey() {
        return clientPublicKey;
    }

    public boolean isAuthenticated() {
        return username != null;
    }

    @Override
    public String toString() {
        return username != null ? username : socket.getRemoteSocketAddress().toString();
    }

    public void send(String message) {
        sendChat(message);
    }

    public void broadcast(String message) {
        server.broadcast(this, message);
    }


    private void handleFileAccept(String filename) {
        PendingFile pending = server.getPendingFile(filename);

        if (pending == null) {
            send("No pending file named '" + filename + "'");
            return;
        }

        if (pending.getRecipient() != this) {
            send("You are not the recipient of this file.");
            return;
        }

        try {
            // ✅ Deliver the file to the recipient
            pending.sendToRecipient();

            // ✅ Remove from pending list after sending
            server.removePendingFile(filename);

            // Notify sender that the file was accepted
            ClientHandler sender = pending.getSender();
            sender.send("User '" + this + "' accepted your file '" + filename + "'.");

            // Minimal confirmation for recipient
            send("You received the file '" + filename + "'.");

            // Clean up the temporary file
            pending.cleanup();

        } catch (Exception e) {
            Logger.error("Failed to deliver file", e);
            send("Failed to receive file.");
        }
    }


    private void handleFileReject(String filename) {
        PendingFile pending = server.getPendingFile(filename);

        if (pending == null) {
            send("No pending file named '" + filename + "'");
            return;
        }

        if (pending.getRecipient() != this) {
            send("You are not the recipient of this file.");
            return;
        }

        // Notify sender about rejection
        ClientHandler sender = pending.getSender();
        sender.send("User '" + this + "' rejected your file '" + filename + "'.");

        // Notify recipient that the file was rejected
        send("You rejected the file '" + filename + "'.");

        // Clean up the temporary file
        pending.cleanup();

        // Remove from pending list
        server.removePendingFile(filename);
    }
}
