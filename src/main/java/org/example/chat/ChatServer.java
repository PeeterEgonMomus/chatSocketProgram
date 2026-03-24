package org.example.chat;

import org.example.chat.auth.*;
import org.example.chat.auth.policy.*;
import org.example.chat.commands.*;
import org.example.chat.files.FileTransferManager;
import org.example.chat.files.ServerFileTransferService;
import org.example.chat.games.*;
import org.example.chat.games.commands.*;
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
    private final ServerFileTransferService fileTransfers;

    private final FrameRouter router = new FrameRouter();
    private final HandshakeService handshakeService = new RSAHandshakeService();

    private final LeaderboardManager leaderboardManager = new LeaderboardManager();
    private final GameManager gameManager = new GameManager(leaderboardManager);
    private final GameRegistry gameRegistry = new GameRegistry();

    // ✅ SINGLE ENTRY POINT FOR ALL GAME LOGIC
    private final GameService gameService;

    // ✅ Command-side action router
    private GameActionRegistry gameActionRegistry;

    private final ExecutorService transferExecutor = Executors.newFixedThreadPool(8);

    public ChatServer(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;

        this.fileTransferManager = new FileTransferManager(transferExecutor);
        this.fileTransfers = new ServerFileTransferService(fileTransferManager);

        // ✅ Build service AFTER dependencies
        this.gameService = new GameService(gameManager, gameRegistry, leaderboardManager);

        registerAuth();
        registerGames();
        registerCommands();
        registerFrameHandlers();
    }

    // =========================
    // 🔐 AUTH CONFIGURATION
    // =========================

    private void registerAuth() {

        PasswordRule passwordRule = new CompositePasswordRule()
                .addRule(new MinLengthRule(8))
                .addRule(new MustContainNumberRule());

        registry.register(new RegisterCommand(userStore, passwordRule));
        registry.register(new LoginCommand(userStore, sessionManager));
    }

    // =========================
    // 💬 COMMANDS
    // =========================

    private void registerCommands() {

        registry.register(new HelpCommand(registry));
        registry.register(new BroadcastCommand());

        // =========================
        // 🎮 GAME ACTION SYSTEM
        // =========================

        gameActionRegistry = new GameActionRegistry();

        gameActionRegistry.register(new InviteAction(gameService));
        gameActionRegistry.register(new AcceptAction(gameService));
        gameActionRegistry.register(new DeclineAction(gameService));
        gameActionRegistry.register(new RematchAction(gameService));
        gameActionRegistry.register(new StatsAction(gameService));
        gameActionRegistry.register(new LeaderboardAction(gameService));
        gameActionRegistry.register(new ListAction(gameService));
        gameActionRegistry.register(new HelpAction(gameService));

        registry.register(new GameCommand(gameActionRegistry));

        // ✅ FIXED: now goes through GameService
        registry.register(new MoveCommand(gameService));
    }

    // =========================
    // 🎮 GAMES
    // =========================

    private void registerGames() {

        gameRegistry.register(new RockPaperScissorsGame());
        gameRegistry.register(new CoinFlipGame());
    }

    // =========================
    // 📡 FRAME HANDLERS
    // =========================

    private void registerFrameHandlers() {

        router.register(new ChatFrameHandler());

        // File transfer
        router.register(new SendFileRequestHandler(fileTransfers));
        router.register(new FileOfferHandler(fileTransfers));
        router.register(new FileStartHandler(fileTransfers));
        router.register(new FileChunkHandler(fileTransfers));
        router.register(new FileEndHandler(fileTransfers));
        router.register(new FileAcceptHandler(fileTransfers));
        router.register(new FileRejectHandler(fileTransfers));

        // =========================
        // 🎮 GAME FRAME HANDLERS
        // =========================

        router.register(new GameInviteHandler(gameService));
        router.register(new GameAcceptHandler(gameService));
        router.register(new GameDeclineHandler(gameService));

        // ⚠️ still bypassing GameService (next step)
        router.register(new GameMoveHandler());
    }

    // =========================
    // 🚀 SERVER LIFECYCLE
    // =========================

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

    public void shutdown() {
        transferExecutor.shutdown();
    }

    // =========================
    // 📢 BROADCAST
    // =========================

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

    // =========================
    // 🔍 GETTERS
    // =========================

    public FileTransferManager getFileTransferManager() {
        return fileTransferManager;
    }

    public ExecutorService getTransferExecutor() {
        return transferExecutor;
    }

    public CommandRegistry getRegistry() {
        return registry;
    }

    public EncryptionService getEncryptionService() {
        return encryptionService;
    }

    public UserSessionManager getSessionManager() {
        return sessionManager;
    }

    public List<ClientHandler> getAllClients() {
        return Collections.unmodifiableList(clients);
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public GameRegistry getGameRegistry() {
        return gameRegistry;
    }

    public GameService getGameService() {
        return gameService;
    }

    // =========================
    // 🏁 ENTRY POINT
    // =========================

    public static void main(String[] args) throws IOException {
        EncryptionService encryption =
                new EncryptionService(new HybridEncryption(new RSAEncryption()));

        new ChatServer(encryption).start(12345);
    }
}