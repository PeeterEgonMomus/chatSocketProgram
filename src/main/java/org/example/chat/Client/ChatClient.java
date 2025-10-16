package org.example.chat.Client;

import org.example.chat.util.Logger;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;

public class ChatClient {
    private final String host;
    private final int port;
    private final ClientCrypto encryptionService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chat-client-reader");
        t.setDaemon(true);
        return t;
    });

    // Optional injected connection (for testing)
    private final ChatConnection connectionOverride;

    // New: file transfer support
    private final FileTransferClient fileTransferClient;

    public ChatClient(String host, int port, ClientCrypto encryptionService) {
        this(host, port, encryptionService, null);
    }

    public ChatClient(String host, int port, ClientCrypto encryptionService, ChatConnection connectionOverride) {
        this.host = host;
        this.port = port;
        this.encryptionService = encryptionService;
        this.connectionOverride = connectionOverride;
        this.fileTransferClient = new FileTransferClient(host, port, encryptionService);
    }

    public void start() {
        try (ChatConnection connection = (connectionOverride != null)
                ? connectionOverride
                : new SocketChatConnection(new Socket(host, port));
             BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in))) {

            Logger.info("Connected to server " + host + ":" + port);

            HandshakeManager handshake = new HandshakeManager(encryptionService, connection);
            if (!handshake.doClientHandshake()) {
                Logger.error("Handshake failed. Aborting connection.", new RuntimeException("Handshake failed"));
                return;
            }

            // Reader thread
            executor.submit(() -> {
                try {
                    String line;
                    while ((line = connection.receive()) != null) {
                        handleIncoming(line);
                    }
                } catch (IOException e) {
                    Logger.error("Connection closed by server", e);
                }
            });

            // Send loop
            String input;
            while ((input = userIn.readLine()) != null) {
                if (input.equalsIgnoreCase("/quit")) break;

                // New: handle /sendfile <recipient> <path>
                if (input.startsWith("/sendfile ")) {
                    String[] parts = input.split(" ", 3);
                    if (parts.length < 3) {
                        System.out.println("Usage: /sendfile <recipient> <path>");
                        continue;
                    }
                    String recipient = parts[1];
                    Path filePath = Path.of(parts[2]);
                    fileTransferClient.sendFile(filePath, recipient);
                    continue;
                }

                try {
                    String encrypted = encryptionService.encryptForServer(input);
                    connection.send(encrypted);
                } catch (Exception e) {
                    Logger.error("Encryption or send failed", e);
                }
            }

        } catch (Exception e) {
            Logger.error("Connection error", e);
        } finally {
            shutdownExecutor();
        }
    }

    private void handleIncoming(String line) {
        try {
            String decrypted;
            if (encryptionService.isAESReady()) {
                decrypted = encryptionService.decryptFromServer(line);
            } else if ("AES_OK".equals(line)) {
                encryptionService.markAESReady();
                System.out.println("[Handshake complete] AES session ready!");
                return;
            } else {
                // Before AES ready — may still be plaintext messages
                System.out.println("[Server] " + line);
                return;
            }

            // ===============================
            // 🔽  FILE TRANSFER REQUEST
            // ===============================
            if (decrypted.startsWith("FILE_REQUEST:")) {
                String[] parts = decrypted.split(":", 4);
                if (parts.length < 4) {
                    Logger.error("Invalid FILE_REQUEST message: " + decrypted, null);
                    return;
                }

                String sender = parts[1];
                String filename = parts[2];
                int size = Integer.parseInt(parts[3]);

                System.out.println("📁 Incoming file from " + sender +
                        ": " + filename + " (" + size + " bytes)");
                System.out.print("Accept file? (y/n): ");

                // Ask user interactively if they want to accept
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String response = reader.readLine();

                if (response != null && response.trim().equalsIgnoreCase("y")) {
                    System.out.println("Starting file download...");
                    fileTransferClient.receiveFile(filename, size);
                } else {
                    System.out.println("File declined.");
                    // Optionally notify server you declined
                    // connection.send(encryptionService.encryptForServer("FILE_DECLINE:" + sender + ":" + filename));
                }

                return;
            }

            // ===============================
            // 🔽  DEFAULT CHAT MESSAGE
            // ===============================
            System.out.println(decrypted);

        } catch (Exception e) {
            System.out.println("(raw) " + line);
            Logger.error("Failed to decrypt or handle server message", e);
        }
    }


    private void shutdownExecutor() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        String host = "localhost";
        int port = 12345;

        if (args.length >= 1) host = args[0];
        if (args.length >= 2) port = Integer.parseInt(args[1]);

        ClientCrypto crypto = new ClientEncryption();
        new ChatClient(host, port, crypto).start();
    }
}
