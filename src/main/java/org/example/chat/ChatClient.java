package org.example.chat;

import org.example.chat.util.Logger;
import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class ChatClient {
    private final String host;
    private final int port;
    private final ClientEncryption encryptionService;

    public ChatClient(String host, int port, ClientEncryption encryptionService) {
        this.host = host;
        this.port = port;
        this.encryptionService = encryptionService;
    }

    public void start() {
        try (Socket socket = new Socket(host, port);
             BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in))) {

            Logger.info("Connected to server " + host + ":" + port);

            // --- Step 1: Receive server public key ---
            String firstLine = serverIn.readLine();
            if (firstLine == null || !firstLine.startsWith("PUBLIC_KEY:")) {
                Logger.error("Server did not send public key properly", new RuntimeException("Missing PUBLIC_KEY"));
                return;
            }
            String serverPub = firstLine.substring("PUBLIC_KEY:".length()).trim();
            encryptionService.setServerPublicKeyBase64(serverPub);
            Logger.debug("Received server public key (" + serverPub.length() + " chars)");

            // --- Step 2: Send client public key ---
            String clientPub = encryptionService.getPublicKeyBase64();
            serverOut.println("CLIENT_KEY:" + clientPub);
            Logger.debug("Sent client public key to server");

            // --- Step 3: Generate AES key and send RSA-encrypted ---
            String aesKey = encryptionService.generateAESKeyBase64();
            Logger.debug("Generated AES key: " + aesKey.substring(0, 20) + "...");
            String encryptedAESKey = encryptionService.encryptForServerRSA(aesKey);
            Logger.debug("Encrypted AES key length: " + encryptedAESKey.length());
            serverOut.println("AES_KEY:" + encryptedAESKey);
            Logger.debug("Sent AES key to server (RSA-encrypted)");

            // --- Step 4: Start reader thread ---
            Thread readerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = serverIn.readLine()) != null) {
                        try {
                            if (encryptionService.isAESReady()) {
                                String decrypted = encryptionService.decryptFromServer(line);
                                Logger.debug("Decrypted server message: " + decrypted);
                                System.out.println(decrypted);
                            } else {
                                if ("AES_OK".equals(line)) {
                                    encryptionService.markAESReady();
                                    Logger.info("Handshake complete: AES session ready!");
                                    System.out.println("[Handshake complete] AES session ready!");
                                } else {
                                    Logger.debug("Received unencrypted or pending message: " + line);
                                    System.out.println("[Server] " + line);
                                }
                            }
                        } catch (Exception e) {
                            Logger.error("Failed to decrypt server message", e);
                            System.out.println("(raw) " + line);
                        }
                    }
                } catch (IOException e) {
                    Logger.error("Connection closed by server", e);
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            // --- Step 5: Main send loop ---
            String input;
            while ((input = userIn.readLine()) != null) {
                if (input.equalsIgnoreCase("/quit")) break;

                try {
                    String encrypted = encryptionService.encryptForServer(input);
                    Logger.debug("Sending encrypted message (" + encrypted.length() + " chars)");
                    serverOut.println(encrypted);
                } catch (Exception e) {
                    Logger.error("Encryption or send failed", e);
                }
            }

        } catch (Exception e) {
            Logger.error("Connection error", e);
        }
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        String host = "localhost";
        int port = 12345;

        if (args.length >= 1) host = args[0];
        if (args.length >= 2) port = Integer.parseInt(args[1]);

        ClientEncryption encryptionService = new ClientEncryption();
        new ChatClient(host, port, encryptionService).start();
    }
}
