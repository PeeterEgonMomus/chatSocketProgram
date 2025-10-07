package org.example.chat;

import org.example.chat.security.EncryptionService;
import org.example.chat.util.Logger;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private final EncryptionService encryptionService;
    private PrintWriter out;
    private String username;
    private String clientPublicKey; // base64 public key of this client

    public ClientHandler(Socket socket, ChatServer server, EncryptionService encryptionService) {
        this.socket = socket;
        this.server = server;
        this.encryptionService = encryptionService;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);

            // 1) immediately send server public key to client (plaintext)
            String serverPub = server.getEncryptionService().getServerPublicKeyBase64();
            out.println("PUBLIC_KEY:" + serverPub);
            Logger.debug("Sent PUBLIC_KEY to client " + socket.getRemoteSocketAddress());

            String input;
            while ((input = in.readLine()) != null) {
                // handshake step: client public key
                if (input.startsWith("CLIENT_KEY:")) {
                    String clientKeyBase64 = input.substring("CLIENT_KEY:".length()).trim();
                    this.clientPublicKey = clientKeyBase64;
                    server.registerClientPublicKey(this, clientKeyBase64);
                    Logger.debug("Registered client public key for " + socket.getRemoteSocketAddress());
                    continue;
                }

                // handshake step (HYBRID): client sends its AES key encrypted with server RSA pub
                if (input.startsWith("AES_KEY:")) {
                    String encryptedAESKey = input.substring("AES_KEY:".length()).trim();
                    encryptionService.registerClientAESKey(this, encryptedAESKey);
                    Logger.debug("Registered AES session key for " + socket.getRemoteSocketAddress());
                    // Ack that AES is set (server encrypts ack with AES if possible)
                    try {
                        String ack = "AES_OK";
                        String cipher = encryptionService.encryptForClient(this, ack, clientPublicKey);
                        out.println(cipher);
                    } catch (Exception e) {
                        out.println("ERROR: AES registration failed");
                    }
                    continue;
                }

                // decrypt payload (uses AES if registered, otherwise server RSA decrypt)
                String decrypted;
                try {
                    decrypted = encryptionService.decryptFromClient(this, input);
                } catch (RuntimeException e) {
                    Logger.error("Failed to decrypt incoming payload", e);
                    out.println("ERROR: unable to decrypt message");
                    continue;
                }

                Logger.debug("Decrypted input from client " + socket.getRemoteSocketAddress() + ": " +
                        (decrypted.length() > 80 ? decrypted.substring(0,80) + "..." : decrypted));
                server.getRegistry().executeCommand(this, decrypted);
            }
        } catch (IOException e) {
            Logger.error("Client disconnected: " + e.getMessage(), e);
            server.removeClient(this);
        } finally {
            encryptionService.removeClient(this);
        }
    }

    /**
     * Send a message to this client. If client has provided an AES key, it will encrypt with AES.
     * Otherwise it falls back to RSA with the client's public key (if available).
     */
    public void send(String message) {
        try {
            String cipher = encryptionService.encryptForClient(this, message, clientPublicKey);
            out.println(cipher);
        } catch (Exception e) {
            // if encryption failed, fallback to plaintext for debugging (not recommended in production)
            Logger.error("Failed to encrypt outbound message, sending plaintext fallback", e);
            out.println(message);
        }
    }

    public ChatServer getServer() {
        return server;
    }

    @Override
    public String toString() {
        return username != null ? username : socket.getRemoteSocketAddress().toString();
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAuthenticated() {
        return username != null && !username.isEmpty();
    }
}
