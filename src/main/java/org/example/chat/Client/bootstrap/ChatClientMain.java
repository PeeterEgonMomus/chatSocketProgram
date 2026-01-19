package org.example.chat.Client.bootstrap;

import org.example.chat.Client.command.ConsoleCommandProcessor;
import org.example.chat.Client.connection.ConnectionManager;
import org.example.chat.Client.crypto.ClientCrypto;
import org.example.chat.Client.crypto.ClientEncryption;
import org.example.chat.Client.protocol.FrameDispatcher;
import org.example.chat.Client.protocol.HandshakeService;
import org.example.chat.Client.runtime.ClientRuntime;

public final class ChatClientMain {

    public static void main(String[] args) {
        try {
            // 1️⃣ Crypto
            ClientCrypto crypto = new ClientEncryption();

            // 2️⃣ Handshake service
            HandshakeService handshakeService =
                    new HandshakeService(crypto);

            // 3️⃣ Dispatcher
            FrameDispatcher dispatcher = new FrameDispatcher();

            // 4️⃣ Command processor
            ConsoleCommandProcessor commandProcessor =
                    new ConsoleCommandProcessor(crypto);

            // 5️⃣ Connection manager
            ConnectionManager connectionManager =
                    new ConnectionManager(
                            "localhost",
                            12345,
                            handshakeService
                    );

            // 6️⃣ Runtime
            ClientRuntime runtime = new ClientRuntime(
                    connectionManager,
                    dispatcher,
                    commandProcessor,
                    crypto
            );

            // 7️⃣ Client facade
            ChatClient client = new ChatClient(runtime);

            // 8️⃣ Start client
            client.start();

            // 🟢 KEEP JVM ALIVE
            Thread.currentThread().join();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start chat client");
        }
    }
}
