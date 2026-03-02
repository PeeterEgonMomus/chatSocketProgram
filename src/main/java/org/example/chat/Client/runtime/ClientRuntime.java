package org.example.chat.Client.runtime;

import org.example.chat.Client.ClientMessageGateway;
import org.example.chat.Client.CommandInputSource;
import org.example.chat.Client.FramedConnectionGateway;
import org.example.chat.Client.SystemConsoleInputSource;
import org.example.chat.Client.command.CommandProcessor;
import org.example.chat.Client.command.ConsoleCommandProcessor;
import org.example.chat.Client.command.strategy.CommandRegistry;
import org.example.chat.Client.command.strategy.CommandRegistryBuilder;
import org.example.chat.Client.connection.ConnectionManager;
import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.Client.crypto.ClientCrypto;
import org.example.chat.Client.crypto.ClientEncryption;
import org.example.chat.Client.file.FileTransferService;
import org.example.chat.Client.file.IncomingFileState;
import org.example.chat.Client.file.IncomingTransferRegistry;
import org.example.chat.Client.protocol.ClientHandlerBootstrap;
import org.example.chat.Client.protocol.DefaultClientCipher;
import org.example.chat.Client.protocol.FrameDispatcher;
import org.example.chat.Client.protocol.HandshakeService;
import org.example.chat.protocol.Frame;
import org.example.chat.util.Logger;


import javax.crypto.SecretKey;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ClientRuntime {

    private final ConnectionManager connectionManager;
    private final FrameDispatcher dispatcher;

    private FramedChatConnection connection;
    private CommandProcessor commandProcessor;



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
     * Starts the client runtime: connects, performs handshake, sets up encryption,
     * registers frame handlers, starts command processor and frame reader.
     */
    public void start() throws Exception {

        // 1️⃣ Connect (handshake runs internally)
        this.connection = connectionManager.connect();

        // 2️⃣ Retrieve AES key from connection manager
        SecretKey aesKey = connectionManager.getSessionAESKey();

        // 3️⃣ Create session encryption
        ClientEncryption encryption = new ClientEncryption(aesKey);

        // 4️⃣ Attach cipher to connection (AES ready)
        connection.setCipher(new DefaultClientCipher(encryption));

        Logger.info("Handshake completed, AES ready");

        // 5️⃣ Shared services
        FileTransferService transferService = new FileTransferService(connection);
        IncomingTransferRegistry registry = new IncomingTransferRegistry();
        ClientMessageGateway gateway = new FramedConnectionGateway(connection);

        CommandInputSource inputSource =
                new SystemConsoleInputSource();


        // 6️⃣ Command processor
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

        // 7️⃣ Register frame handlers
        ClientHandlerBootstrap.registerAll(dispatcher, registry, transferService, connection);

        // 8️⃣ Start command processor
        commandProcessor.start(connection);

        // 9️⃣ Start frame reader
        startReader();
    }


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
     * Gracefully stops the client runtime
     */
    public void stop() {
        try {
            readerExecutor.shutdownNow();
            if (dispatcher != null) dispatcher.stop();
            if (commandProcessor != null) commandProcessor.stop();
            if (connectionManager != null) connectionManager.close();
        } catch (Exception ignored) {}
    }

    public FramedChatConnection getConnection() {
        return connection;
    }
}