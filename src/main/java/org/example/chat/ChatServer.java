package org.example.chat;

import org.example.chat.auth.InMemoryUserStore;
import org.example.chat.auth.UserStore;
import org.example.chat.auth.policy.*;
import org.example.chat.commands.*;
import org.example.chat.file.FileTransferServer; // ✅ NEW
import org.example.chat.security.EncryptionService;
import org.example.chat.security.HybridEncryption;
import org.example.chat.security.RSAEncryption;
import org.example.chat.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private final List<ClientHandler> clients = new ArrayList<>();
    private final CommandRegistry registry = new CommandRegistry();
    private final UserStore userStore = new InMemoryUserStore();
    private final EncryptionService encryptionService;
    private final Map<ClientHandler, String> clientPublicKeys = new ConcurrentHashMap<>();

    public ChatServer(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;

        PasswordPolicy passwordPolicy = new CompositePasswordPolicy()
                .addPolicy(new MinLengthPolicy(8))
                .addPolicy(new MustContainNumberPolicy());

        registry.register(new RegisterCommand(userStore, passwordPolicy));
        registry.register(new LoginCommand(userStore));
        registry.register(new HelpCommand(registry));
        registry.register(new BroadcastCommand());
    }

    public void start(int port) throws IOException {
        // ✅ Start the file transfer server on a secondary port (port + 1)
        new Thread(new FileTransferServer(this, port), "file-transfer-server").start();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Logger.info("Chat server started on port " + port);
            Logger.info("File transfer server running on port " + (port + 1));

            while (true) {
                Socket socket = serverSocket.accept();
                Logger.info("New client connected: " + socket.getRemoteSocketAddress());
                ClientHandler client = new ClientHandler(socket, this, encryptionService);
                clients.add(client);
                new Thread(client).start();
            }
        }
    }

    /** Broadcast message to all clients except sender. */
    public void broadcast(ClientHandler sender, String message) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.send("BROADCAST from " + sender + ": " + message);
            }
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        clientPublicKeys.remove(client);
    }

    public CommandRegistry getRegistry() {
        return registry;
    }

    public EncryptionService getEncryptionService() {
        return encryptionService;
    }

    public List<ClientHandler> getAllClients() {
        return Collections.unmodifiableList(clients);
    }


    /** Called by ClientHandler when a client supplies its public key during handshake. */
    public void registerClientPublicKey(ClientHandler client, String clientPublicKeyBase64) {
        clientPublicKeys.put(client, clientPublicKeyBase64);
    }

    // ✅ Added helper for file transfer logic
    public ClientHandler findClientByUsername(String username) {
        for (ClientHandler client : clients) {
            if (username.equalsIgnoreCase(client.getUsername())) {
                return client;
            }
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        EncryptionService encryption = new EncryptionService(new HybridEncryption(new RSAEncryption()));
        new ChatServer(encryption).start(12345);
    }
}
