package org.example.chat.Client.bootstrap;

import org.example.chat.Client.command.ConsoleCommandProcessor;
import org.example.chat.Client.connection.ConnectionManager;
import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.Client.crypto.ClientCrypto;
import org.example.chat.Client.crypto.ClientEncryption;
import org.example.chat.Client.crypto.ClientHandshakeCipher;
import org.example.chat.Client.protocol.DefaultClientCipher;
import org.example.chat.Client.protocol.FrameDispatcher;
import org.example.chat.Client.protocol.HandshakeService;
import org.example.chat.Client.runtime.ClientRuntime;

import javax.crypto.SecretKey;

/**
 * Client Bootstrapping Entry Point.
 *
 * Responsibility:
 * - Wires together all client components.
 * - Creates encryption, handshake, dispatcher, and runtime.
 * - Starts the client application.
 *
 * Architecture Role:
 * This is the composition root of the client.
 *
 * All dependencies are constructed here.
 * No dependency creation should occur deeper in the system.
 *
 * Layering:
 *
 * Bootstrap
 *     ↓
 * Runtime
 *     ↓
 * ConnectionManager
 *     ↓
 * Protocol / Dispatcher
 *     ↓
 * Domain Logic
 *
 * Design Principles:
 * - Explicit dependency wiring
 * - No hidden instantiation
 * - Clear startup flow
 *
 * This class does NOT:
 * - Contain client logic
 * - Handle protocol
 * - Manage threads directly
 *
 * It only assembles the system.
 */
public final class ChatClientMain {

    public static void main(String[] args) {
        try {

            /*
             * Create client-side handshake crypto.
             * Responsible for RSA key exchange and AES session setup.
             */
            ClientHandshakeCipher handshakeCrypto =
                    new ClientHandshakeCipher();

            /*
             * Handshake service orchestrates the key exchange protocol.
             */
            HandshakeService handshakeService =
                    new HandshakeService(handshakeCrypto);

            /*
             * Dispatcher routes incoming frames
             * to appropriate client-side handlers.
             */
            FrameDispatcher dispatcher = new FrameDispatcher();

            /*
             * Connection manager handles:
             * - TCP connection
             * - Performing handshake
             * - Creating framed connection
             */
            ConnectionManager connectionManager =
                    new ConnectionManager(
                            "localhost",
                            12345,
                            handshakeService
                    );

            /*
             * ClientRuntime coordinates:
             * - Connection lifecycle
             * - Frame dispatching
             * - Background threads
             */
            ClientRuntime runtime =
                    new ClientRuntime(
                            connectionManager,
                            dispatcher
                    );

            runtime.start();

            /*
             * Prevents JVM from exiting.
             * Keeps client running until externally terminated.
             */
            Thread.currentThread().join();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start chat client");
        }
    }
}