package org.example.chat.Client;// package org.example.chat;

import org.example.chat.util.Logger;
import java.io.*;
import java.net.Socket;
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

    public ChatClient(String host, int port, ClientCrypto encryptionService) {
        this(host, port, encryptionService, null);
    }

    public ChatClient(String host, int port, ClientCrypto encryptionService, ChatConnection connectionOverride) {
        this.host = host;
        this.port = port;
        this.encryptionService = encryptionService;
        this.connectionOverride = connectionOverride;
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
            if (encryptionService.isAESReady()) {
                String decrypted = encryptionService.decryptFromServer(line);
                System.out.println(decrypted);
            } else if ("AES_OK".equals(line)) {
                encryptionService.markAESReady();
                System.out.println("[Handshake complete] AES session ready!");
            } else {
                System.out.println("[Server] " + line);
            }
        } catch (Exception e) {
            System.out.println("(raw) " + line);
            Logger.error("Failed to decrypt server message", e);
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
