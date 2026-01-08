package org.example.chat;

import org.example.chat.auth.*;
import org.example.chat.auth.policy.*;
import org.example.chat.commands.*;
import org.example.chat.security.EncryptionService;
import org.example.chat.security.HybridEncryption;
import org.example.chat.security.RSAEncryption;
import org.example.chat.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class ChatServer {
    private final List<ClientHandler> clients = new ArrayList<>();
    private final Map<String, PendingFile> pendingFiles = new HashMap<>();
    private final CommandRegistry registry = new CommandRegistry();
    private final UserStore userStore = new InMemoryUserStore();
    private final UserSessionManager sessionManager = new UserSessionManager();
    private final EncryptionService encryptionService;

    public ChatServer(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;

        PasswordPolicy passwordPolicy = new CompositePasswordPolicy()
                .addPolicy(new MinLengthPolicy(8))
                .addPolicy(new MustContainNumberPolicy());

        registry.register(new RegisterCommand(userStore, passwordPolicy));
        registry.register(new LoginCommand(userStore, sessionManager)); // inject sessionManager
        registry.register(new HelpCommand(registry));
        registry.register(new BroadcastCommand());
    }

    public void start(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Logger.info("Chat server started on port " + port);

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
        sessionManager.getSessionBySocket(client.getSocket())
                .ifPresent(session -> sessionManager.removeSession(session.getUsername()));
    }

    public void addPendingFile(String filename, PendingFile pendingFile) {
        pendingFiles.put(filename, pendingFile);
        Logger.debug("Pending file registered: " + filename);
    }

    public PendingFile getPendingFile(String filename) {
        return pendingFiles.get(filename);
    }

    public PendingFile removePendingFile(String filename) {
        PendingFile removed = pendingFiles.remove(filename);
        if (removed != null) {
            Logger.debug("Pending file removed: " + filename);
        }
        return removed;
    }

    public CommandRegistry getRegistry() { return registry; }
    public EncryptionService getEncryptionService() { return encryptionService; }
    public UserSessionManager getSessionManager() { return sessionManager; }
    public List<ClientHandler> getAllClients() { return Collections.unmodifiableList(clients); }

    public static void main(String[] args) throws IOException {
        EncryptionService encryption = new EncryptionService(new HybridEncryption(new RSAEncryption()));
        new ChatServer(encryption).start(12345);
    }
}
