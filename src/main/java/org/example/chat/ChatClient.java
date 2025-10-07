package org.example.chat;

import org.example.chat.security.RSAClient;
import org.example.chat.security.AESEncryption;
import org.example.chat.util.Logger;

import java.io.*;
import java.net.Socket;

public class ChatClient {
    private final String host;
    private final int port;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        final RSAClient rsaClient = new RSAClient();

        try (Socket socket = new Socket(host, port);
             BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to chat server at " + host + ":" + port);

            // === Step 1: Receive server's RSA public key ===
            String firstLine = serverIn.readLine();
            if (firstLine == null || !firstLine.startsWith("PUBLIC_KEY:")) {
                System.err.println("Protocol error: expected PUBLIC_KEY from server.");
                return;
            }
            String serverPub = firstLine.substring("PUBLIC_KEY:".length()).trim();
            rsaClient.setServerPublicKeyBase64(serverPub);
            Logger.debug("Received server public key.");

            // === Step 2: Send our public key ===
            String clientPubBase64 = rsaClient.getPublicKeyBase64();
            serverOut.println("CLIENT_KEY:" + clientPubBase64);
            Logger.debug("Sent CLIENT_KEY to server.");

            // === Step 3: Generate AES session key ===
            String aesKeyBase64 = AESEncryption.generateKeyBase64();
            Logger.debug("Generated AES session key.");

            // === Step 4: Encrypt AES key with RSA and send ===
            String encryptedAESKey = rsaClient.encryptForServer(aesKeyBase64);
            serverOut.println("AES_KEY:" + encryptedAESKey);
            Logger.debug("Sent AES_KEY to server (encrypted with RSA).");

            final String finalAesKey = aesKeyBase64;
            final BufferedReader finalServerIn = serverIn;

            // === Step 5: Reader thread (AES decrypt) ===
            Thread readerThread = new Thread(() -> {
                String line;
                try {
                    while ((line = finalServerIn.readLine()) != null) {
                        try {
                            String decrypted = AESEncryption.decryptWithKeyBase64(finalAesKey, line);
                            System.out.println(decrypted);
                        } catch (Exception e) {
                            Logger.error("Failed to decrypt message; showing raw", e);
                            System.out.println("(raw) " + line);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Connection closed.");
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            // === Step 6: Main send loop (AES encrypt) ===
            String input;
            while ((input = userIn.readLine()) != null) {
                String encrypted = AESEncryption.encryptWithKeyBase64(finalAesKey, input);
                serverOut.println(encrypted);
            }

        } catch (IOException e) {
            System.err.println("Unable to connect to server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;
        if (args.length >= 1) host = args[0];
        if (args.length >= 2) port = Integer.parseInt(args[1]);

        new ChatClient(host, port).start();
    }
}
