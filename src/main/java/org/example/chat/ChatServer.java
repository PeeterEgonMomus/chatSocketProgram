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
import org.example.chat.heartbeat.HeartbeatMonitor;
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
import java.util.concurrent.ScheduledExecutorService;


/**
 * Design choice:
 * Central composition root of the entire application.
 *
 * ChatServer is responsible for:
 *
 * - Wiring together all subsystems
 * - Owning shared infrastructure
 * - Managing client lifecycle
 * - Bootstrapping the server
 *
 * It acts as the application's Dependency Assembly Layer.
 *
 * ---------------------------------------------------------
 * Architectural Role
 * ---------------------------------------------------------
 *
 * ChatServer does NOT contain:
 * - Game logic
 * - Authentication logic
 * - Encryption logic
 * - File transfer logic
 *
 * Instead, it composes and connects:
 *
 *  Protocol Layer
 *  Security Layer
 *  Authentication Layer
 *  Command Layer
 *  Game Domain
 *  File Transfer Subsystem
 *
 * This follows the Composition Root pattern.
 *
 * ---------------------------------------------------------
 * Why this is important:
 * ---------------------------------------------------------
 *
 * All dependencies are created in ONE place.
 * Nothing instantiates dependencies deep inside the system.
 *
 * This ensures:
 * - Clear ownership
 * - Testability
 * - Clean layering
 * - No circular dependencies
 *
 * ---------------------------------------------------------
 * Concurrency Model:
 * ---------------------------------------------------------
 *
 * - Each client runs in its own thread (ClientHandler).
 * - File transfers use a dedicated thread pool.
 * - Game sessions use scheduled executors for timers.
 *
 * ChatServer coordinates but does not manage fine-grained concurrency.
 *
 * ---------------------------------------------------------
 * High-Level Flow:
 * ---------------------------------------------------------
 *
 * Socket → ClientHandler → FrameRouter → Handler → Service → Domain
 *
 * ChatServer is the top-level orchestrator.
 */
public class ChatServer {

    private final List<ClientHandler> clients = new java.util.concurrent.CopyOnWriteArrayList<>();

    private final CommandRegistry registry = new CommandRegistry();
    private final UserStore userStore = new InMemoryUserStore();
    private final UserSessionManager sessionManager = new UserSessionManager();

    private final EncryptionService encryptionService;
    private final FileTransferManager fileTransferManager;
    private final ServerFileTransferService fileTransfers;

    private final FrameRouter router;
    private final HandshakeService handshakeService;

    private final LeaderboardManager leaderboardManager = new LeaderboardManager();
    private final GameManager gameManager = new GameManager(leaderboardManager);
    private final GameRegistry gameRegistry = new GameRegistry();

    private final PasswordEncoder passwordEncoder;

    // ✅ SINGLE ENTRY POINT FOR ALL GAME LOGIC
    private final GameService gameService;

    // ✅ Command-side action router
    private GameActionRegistry gameActionRegistry;

    private final ExecutorService transferExecutor = Executors.newFixedThreadPool(8);

    // =========================
    // ❤️ HEARTBEAT
    // =========================

    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newSingleThreadScheduledExecutor();

    private final HeartbeatMonitor heartbeatMonitor;

    public ChatServer(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
        this.handshakeService = new RSAHandshakeService(encryptionService);
        this.fileTransferManager = new FileTransferManager(transferExecutor);
        this.fileTransfers = new ServerFileTransferService(fileTransferManager);
        this.router = new FrameRouter(encryptionService);

        this.passwordEncoder =
                new Pbkdf2PasswordEncoder(600_000, 256);
        // 600k iterations, 256-bit key

        // ✅ Build service AFTER dependencies
        this.gameService = new GameService(gameManager, gameRegistry, leaderboardManager);

        registerAuth();
        registerGames();
        registerCommands();
        registerFrameHandlers();

        // =========================
        // ❤️ HEARTBEAT MONITOR
        // =========================

        this.heartbeatMonitor = new HeartbeatMonitor(
                this,
                heartbeatScheduler,
                10_000,  // send PING every 10 seconds
                30_000   // disconnect if no heartbeat for 30 seconds
        );
    }

    // =========================
    // 🔐 AUTH CONFIGURATION
    // =========================

    private void registerAuth() {

        PasswordRule passwordRule = new CompositePasswordRule()
                .addRule(new MinLengthRule(8))
                .addRule(new MustContainNumberRule());

        registry.register(new RegisterCommand(userStore, passwordRule, passwordEncoder));
        registry.register(new LoginCommand(userStore, sessionManager, passwordEncoder));
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

        router.register(new ChatFrameHandler(registry));

        // File transfer
        router.register(new SendFileRequestHandler(fileTransfers));
        router.register(new FileOfferHandler(fileTransfers));
        router.register(new FileStartHandler(fileTransfers));
        router.register(new FileChunkHandler(fileTransfers));
        router.register(new FileEndHandler(fileTransfers));
        router.register(new FileAcceptHandler(fileTransfers));
        router.register(new FileRejectHandler(fileTransfers));

        router.register(new HeartbeatFrameHandler());
        router.register(new PongFrameHandler());

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


            heartbeatMonitor.start();

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
        heartbeatScheduler.shutdown();
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