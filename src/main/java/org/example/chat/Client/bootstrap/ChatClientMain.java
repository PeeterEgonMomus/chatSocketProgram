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

public final class ChatClientMain {

    public static void main(String[] args) {
        try {

            ClientHandshakeCipher handshakeCrypto =
                    new ClientHandshakeCipher();

            HandshakeService handshakeService =
                    new HandshakeService(handshakeCrypto);

            FrameDispatcher dispatcher = new FrameDispatcher();

            ConnectionManager connectionManager =
                    new ConnectionManager(
                            "localhost",
                            12345,
                            handshakeService
                    );

            ClientRuntime runtime =
                    new ClientRuntime(
                            connectionManager,
                            dispatcher
                    );

            runtime.start();

            Thread.currentThread().join();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start chat client");
        }
    }
}
