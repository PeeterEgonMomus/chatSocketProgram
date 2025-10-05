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
                if (input.startsWith("CLIENT_KEY:")) {
                    // client is sending its public key in base64 (plaintext) as part of handshake
                    String clientKeyBase64 = input.substring("CLIENT_KEY:".length()).trim();
                    this.clientPublicKey = clientKeyBase64;
                    server.registerClientPublicKey(this, clientKeyBase64);
                    Logger.debug("Registered client public key for " + socket.getRemoteSocketAddress());
                    continue;
                }

                // All other inbound messages are expected to be encrypted with server public key
                try {
                    String decrypted = encryptionService.decrypt(input);
                    Logger.debug("Decrypted input from client " + socket.getRemoteSocketAddress() + ": " + (decrypted.length() > 80 ? decrypted.substring(0,80) + "..." : decrypted));
                    server.getRegistry().executeCommand(this, decrypted);
                } catch (RuntimeException e) {
                    // decrypt failure (bad padding etc.)
                    Logger.error("Failed to decrypt client message", e);
                    // optionally send error back to client (plaintext) or ignore
                    out.println("ERROR: unable to decrypt message");
                }
            }
        } catch (IOException e) {
            Logger.error("Client disconnected: " + e.getMessage(), e);
            server.removeClient(this);
        }
    }

    /**
     * Send a message to this client. If client has provided a public key,
     * encrypt for that client. Otherwise send plaintext (fallback).
     */
    public void send(String message) {
        if (clientPublicKey != null) {
            String cipher = server.getEncryptionService().encryptWithPublicKey(message, clientPublicKey);
            out.println(cipher);
        } else {
            // fallback - no client key yet (should only happen early)
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
