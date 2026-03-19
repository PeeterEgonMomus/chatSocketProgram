package org.example.chat;

import org.example.chat.auth.*;
import org.example.chat.auth.policy.*;
import org.example.chat.commands.*;
import org.example.chat.files.FileTransferManager;
import org.example.chat.files.ServerFileTransferService;
import org.example.chat.games.GameManager;
import org.example.chat.games.GameRegistry;
import org.example.chat.games.LeaderboardManager;
import org.example.chat.games.RockPaperScissorsGame;
import org.example.chat.handshake.HandshakeService;
import org.example.chat.handshake.RSAHandshakeService;
import org.example.chat.protocol.handlers.*;
import org.example.chat.security.EncryptionService;
import org.example.chat.security.HybridEncryption;
import org.example.chat.security.RSAEncryption;
import org.example.chat.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private final List<ClientHandler> clients = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final CommandRegistry registry = new CommandRegistry();
    private final UserStore userStore = new InMemoryUserStore();
    private final UserSessionManager sessionManager = new UserSessionManager();
    private final EncryptionService encryptionService;
    private final FileTransferManager fileTransferManager;
    private final FrameRouter router = new FrameRouter();
    private final ServerFileTransferService fileTransfers;
    private final HandshakeService handshakeService = new RSAHandshakeService();
    private final LeaderboardManager leaderboardManager = new LeaderboardManager();
    private final GameManager gameManager = new GameManager(leaderboardManager);
    private final GameRegistry gameRegistry = new GameRegistry();



    private final ExecutorService transferExecutor =
            Executors.newFixedThreadPool(8);

    public ChatServer(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;

        this.fileTransferManager = new FileTransferManager(transferExecutor);
        this.fileTransfers = new ServerFileTransferService(fileTransferManager);

        PasswordPolicy passwordPolicy = new CompositePasswordPolicy()
                .addPolicy(new MinLengthPolicy(8))
                .addPolicy(new MustContainNumberPolicy());

        registry.register(new RegisterCommand(userStore, passwordPolicy));
        registry.register(new LoginCommand(userStore, sessionManager));
        registry.register(new HelpCommand(registry));
        registry.register(new BroadcastCommand());

        // ✅ register game commands
        registry.register(new GameCommand(gameManager, gameRegistry, leaderboardManager));
        registry.register(new MoveCommand(gameManager));


        registerFrameHandlers();
        registerGames();
    }

    public FileTransferManager getFileTransferManager() {
        return fileTransferManager;
    }

    public void shutdown() {
        transferExecutor.shutdown();
    }

    public void start(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Logger.info("Chat server started on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                Logger.info("New client connected: " + socket.getRemoteSocketAddress());
                ClientHandler client =
                        new ClientHandler(socket, this, encryptionService, router, handshakeService);
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

    private void registerFrameHandlers() {

        router.register(new ChatFrameHandler());

        router.register(new SendFileRequestHandler(fileTransfers));
        router.register(new FileOfferHandler(fileTransfers));
        router.register(new FileStartHandler(fileTransfers));
        router.register(new FileChunkHandler(fileTransfers));
        router.register(new FileEndHandler(fileTransfers));
        router.register(new FileAcceptHandler(fileTransfers));
        router.register(new FileRejectHandler(fileTransfers));

        // GAME HANDLERS
        router.register(new GameInviteHandler());
        router.register(new GameAcceptHandler());
        router.register(new GameDeclineHandler());
        router.register(new GameMoveHandler());
    }

    private void registerGames() {
        gameRegistry.register(new RockPaperScissorsGame());
    }

    public ExecutorService getTransferExecutor() {
        return transferExecutor;
    }

    public CommandRegistry getRegistry() { return registry; }
    public EncryptionService getEncryptionService() { return encryptionService; }
    public UserSessionManager getSessionManager() { return sessionManager; }
    public List<ClientHandler> getAllClients() { return Collections.unmodifiableList(clients); }
    public GameManager getGameManager() {
        return gameManager;
    }
    public GameRegistry getGameRegistry() {
        return gameRegistry;
    }

    public static void main(String[] args) throws IOException {
        EncryptionService encryption = new EncryptionService(new HybridEncryption(new RSAEncryption()));
        new ChatServer(encryption).start(12345);
    }
}
