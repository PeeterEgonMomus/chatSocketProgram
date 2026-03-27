package org.example.chat.Client.runtime;

import org.example.chat.Client.gateway.ClientMessageGateway;
import org.example.chat.Client.input.CommandInputSource;
import org.example.chat.Client.gateway.FramedConnectionGateway;
import org.example.chat.Client.input.SystemConsoleInputSource;
import org.example.chat.Client.command.CommandProcessor;
import org.example.chat.Client.command.ConsoleCommandProcessor;
import org.example.chat.Client.command.strategy.CommandRegistry;
import org.example.chat.Client.command.strategy.CommandRegistryBuilder;
import org.example.chat.Client.connection.ConnectionManager;
import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.Client.crypto.ClientEncryption;
import org.example.chat.Client.file.FileTransferService;
import org.example.chat.Client.file.IncomingTransferRegistry;
import org.example.chat.Client.protocol.ClientHandlerBootstrap;
import org.example.chat.Client.protocol.DefaultClientCipher;
import org.example.chat.Client.protocol.FrameDispatcher;
import org.example.chat.protocol.Frame;
import org.example.chat.util.Logger;

import javax.crypto.SecretKey;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ClientRuntime orchestrates the entire client lifecycle.
 *
 * Responsibilities:
 * - Establish TCP connection via ConnectionManager
 * - Perform handshake and set up AES encryption
 * - Initialize and attach ClientCipher
 * - Initialize shared services (file transfer, messaging)
 * - Build command registry and start command processor
 * - Register frame handlers
 * - Launch asynchronous frame reader loop
 * - Provide graceful shutdown
 *
 * Architecture Role:
 * - Core runtime layer for the client application
 * - Coordinates transport, protocol dispatch, command processing,
 *   and shared services.
 * - Sits above:
 *      • ConnectionManager (transport layer)
 *      • FramedChatConnection (socket + cipher)
 * - Sits below:
 *      • Console input / user commands
 *      • Command processing strategies
 *      • FrameDispatcher (protocol handler execution)
 *
 * Design Patterns:
 * - Orchestrator / Facade: exposes simple start()/stop() interface
 * - Executor pattern: dedicated thread for frame reading
 * - Dependency Injection: receives ConnectionManager and FrameDispatcher
 *
 * Lifecycle Overview:
 * 1️⃣ Connect → handshake → AES key negotiation
 * 2️⃣ Install cipher → secure channel
 * 3️⃣ Initialize shared services (file transfer, messaging)
 * 4️⃣ Setup commands & registry → command processor
 * 5️⃣ Register all frame handlers
 * 6️⃣ Start command processor
 * 7️⃣ Start frame reader loop
 */
public final class ClientRuntime {

    // Dependencies
    private final ConnectionManager connectionManager;
    private final FrameDispatcher dispatcher;

    // Client components
    private FramedChatConnection connection;
    private CommandProcessor commandProcessor;

    /**
     * Executor for asynchronous frame reading
     * Runs a single daemon thread
     */
    private final ExecutorService readerExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "client-frame-reader");
                t.setDaemon(true);
                return t;
            });

    public ClientRuntime(
            ConnectionManager connectionManager,
            FrameDispatcher dispatcher
    ) {
        this.connectionManager = connectionManager;
        this.dispatcher = dispatcher;
    }

    /**
     * Starts the client runtime
     *
     * Flow:
     * 1️⃣ Connect via ConnectionManager (handshake occurs internally)
     * 2️⃣ Retrieve AES key
     * 3️⃣ Setup ClientEncryption
     * 4️⃣ Attach DefaultClientCipher to connection
     * 5️⃣ Initialize shared services (FileTransfer, Gateway)
     * 6️⃣ Build CommandRegistry and ConsoleCommandProcessor
     * 7️⃣ Register frame handlers with FrameDispatcher
     * 8️⃣ Start command processor loop
     * 9️⃣ Start asynchronous frame reader loop
     */
    public void start() throws Exception {

        // 1️⃣ Establish connection and perform handshake
        this.connection = connectionManager.connect();

        // 2️⃣ Get negotiated AES session key
        SecretKey aesKey = connectionManager.getSessionAESKey();

        // 3️⃣ Setup client-side encryption
        ClientEncryption encryption = new ClientEncryption(aesKey);

        // 4️⃣ Install cipher on transport (secure)
        connection.setCipher(new DefaultClientCipher(encryption));

        Logger.info("Handshake completed, AES ready");

        // 5️⃣ Shared services initialization
        FileTransferService transferService = new FileTransferService(connection);
        IncomingTransferRegistry registry = new IncomingTransferRegistry();
        ClientMessageGateway gateway = new FramedConnectionGateway(connection);
        CommandInputSource inputSource = new SystemConsoleInputSource();

        // 6️⃣ Build command registry & processor
        CommandRegistry commandRegistry =
                CommandRegistryBuilder.build(
                        transferService,
                        registry,
                        connection
                );

        this.commandProcessor =
                new ConsoleCommandProcessor(
                        inputSource,
                        commandRegistry,
                        gateway
                );

        // 7️⃣ Register protocol frame handlers
        ClientHandlerBootstrap.registerAll(dispatcher, registry, transferService, connection);

        // 8️⃣ Start command processor
        commandProcessor.start(connection);

        // 9️⃣ Start asynchronous frame reader
        startReader();
    }

    /**
     * Launches frame reader loop in a dedicated thread
     *
     * Responsibilities:
     * - Continuously reads frames from connection
     * - Dispatches frames via FrameDispatcher
     * - Handles errors by logging and stopping runtime
     */
    private void startReader() {
        readerExecutor.submit(() -> {
            try {
                Frame frame;
                while ((frame = connection.receive()) != null) {
                    dispatcher.dispatch(frame);
                }
            } catch (Exception e) {
                Logger.error("Frame reader stopped", e);
                stop();
            }
        });
    }

    /**
     * Stops the client runtime gracefully.
     *
     * Responsibilities:
     * - Shutdown frame reader thread
     * - Stop dispatcher
     * - Stop command processor
     * - Close transport connection
     */
    public void stop() {
        try {
            readerExecutor.shutdownNow();
            if (dispatcher != null) dispatcher.stop();
            if (commandProcessor != null) commandProcessor.stop();
            if (connectionManager != null) connectionManager.close();
        } catch (Exception ignored) {}
    }

    /**
     * Returns the active framed connection
     */
    public FramedChatConnection getConnection() {
        return connection;
    }
}