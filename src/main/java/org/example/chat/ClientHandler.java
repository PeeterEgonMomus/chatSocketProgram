package org.example.chat;

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

    private OutputStream rawOut;
    private InputStream rawIn;

    private boolean aesReady = false;
    private String username;
    private String clientPublicKey;

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

                case CHAT -> {
                    String encrypted = new String(frame.getPayload(), StandardCharsets.UTF_8);
                    String message = encryptionService.decryptFromClient(this, encrypted);
                    server.getRegistry().executeCommand(this, message);
                }

                case FILE_META -> {
                    Logger.info("Received FILE_META (not handled yet)");
                }

                case FILE_CHUNK -> {
                    Logger.info("Received FILE_CHUNK (not handled yet)");
                }

                case FILE_END -> {
                    Logger.info("Received FILE_END (not handled yet)");
                }

                default -> {
                    Logger.debug("Unhandled frame type: " + frame.getType());
                }
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

}
