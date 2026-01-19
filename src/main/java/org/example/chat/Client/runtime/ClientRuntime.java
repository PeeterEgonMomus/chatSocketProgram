package org.example.chat.Client.runtime;

import org.example.chat.Client.command.CommandProcessor;
import org.example.chat.Client.command.ConsoleCommandProcessor;
import org.example.chat.Client.connection.ConnectionManager;
import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.Client.crypto.ClientCrypto;
import org.example.chat.Client.file.IncomingFileState;
import org.example.chat.Client.protocol.ClientFrameRegistry;
import org.example.chat.Client.protocol.FrameDispatcher;
import org.example.chat.protocol.Frame;
import org.example.chat.util.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ClientRuntime {

    private final ConnectionManager connectionManager;
    private final FrameDispatcher dispatcher;
    private final CommandProcessor commandProcessor;  // concrete type now
    private final ClientCrypto crypto;

    private FramedChatConnection connection;
    private final ExecutorService readerExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "client-frame-reader");
                t.setDaemon(true);
                return t;
            });

    public ClientRuntime(
            ConnectionManager connectionManager,
            FrameDispatcher dispatcher,
            ConsoleCommandProcessor commandProcessor,
            ClientCrypto crypto
    ) {
        this.connectionManager = connectionManager;
        this.dispatcher = dispatcher;
        this.commandProcessor = commandProcessor;
        this.crypto = crypto;
    }

    public void start() throws Exception {
        // 1️⃣ Connect
        this.connection = connectionManager.connect();

        Logger.info("Handshake completed, AES ready");

        // 3️⃣ Register frame handlers (chat, file, etc.)
        IncomingFileState fileState = new IncomingFileState();
        ClientFrameRegistry.registerAll(dispatcher, crypto, fileState);

        // 4️⃣ Start command processor
        commandProcessor.start(connection);

        // 5️⃣ Start frame reader
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

    public void stop() {
        try {
            readerExecutor.shutdownNow();
            if (dispatcher != null) dispatcher.stop();
            if (commandProcessor != null) commandProcessor.stop();
            if (connectionManager != null) connectionManager.close();
        } catch (Exception ignored) {}
    }
}
