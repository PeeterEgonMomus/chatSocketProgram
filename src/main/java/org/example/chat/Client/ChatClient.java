package org.example.chat.Client;

import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;

public class ChatClient {

    private final String host;
    private final int port;
    private final ClientCrypto crypto;

    private FramedChatConnection framedConnection;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "chat-client-reader");
                t.setDaemon(true);
                return t;
            });

    public ChatClient(String host, int port, ClientCrypto crypto) {
        this.host = host;
        this.port = port;
        this.crypto = crypto;
    }

    public void start() {
        try (Socket socket = new Socket(host, port);
             ChatConnection textConn = new SocketChatConnection(socket);
             BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in))) {

            Logger.info("Connected to server");

            // ================= HANDSHAKE (TEXT ONLY) =================
            HandshakeManager handshake = new HandshakeManager(crypto, textConn);
            if (!handshake.doClientHandshake()) {
                return;
            }

            // ================= SWITCH TO FRAMES =================
            framedConnection = new FramedChatConnection(socket);

            executor.submit(() -> readLoop());

            // ================= SEND LOOP =================
            String input;
            while ((input = userIn.readLine()) != null) {
                if (input.equalsIgnoreCase("/quit")) break;

                byte[] encrypted =
                        crypto.encryptForServer(input).getBytes(StandardCharsets.UTF_8);

                Frame frame = new Frame(FrameType.CHAT, encrypted);
                framedConnection.send(frame);
            }

        } catch (Exception e) {
            Logger.error("Client error", e);
        } finally {
            shutdown();
        }
    }

    private void readLoop() {
        try {
            Frame frame;
            while ((frame = framedConnection.receive()) != null) {
                handleFrame(frame);
            }
        } catch (Exception e) {
            Logger.error("Connection closed", e);
        }
    }

    private void handleFrame(Frame frame) {
        try {
            if (frame.getType() == FrameType.CHAT) {
                String encrypted =
                        new String(frame.getPayload(), StandardCharsets.UTF_8);

                String decrypted = crypto.decryptFromServer(encrypted);
                System.out.println(decrypted);
            } else {
                Logger.debug("Unhandled frame type: " + frame.getType());
            }
        } catch (Exception e) {
            Logger.error("Failed to handle frame", e);
        }
    }

    private void shutdown() {
        executor.shutdownNow();
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        new ChatClient("localhost", 12345, new ClientEncryption()).start();
    }
}
