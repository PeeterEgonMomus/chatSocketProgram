package org.example.chat.Client.connection;

import org.example.chat.Client.protocol.DefaultClientCipher;
import org.example.chat.Client.protocol.HandshakeService;

import java.net.Socket;

public final class ConnectionManager implements AutoCloseable {

    private final String host;
    private final int port;
    private final HandshakeService handshakeService;

    private Socket socket;
    private FramedChatConnection framedConnection;

    public ConnectionManager(
            String host,
            int port,
            HandshakeService handshakeService
    ) {
        this.host = host;
        this.port = port;
        this.handshakeService = handshakeService;
    }

    public FramedChatConnection connect() throws Exception {
        socket = new Socket(host, port);

        framedConnection = new FramedChatConnection(socket);

        // plaintext handshake
        handshakeService.perform(framedConnection);

        // 🔐 install cipher AFTER handshake
        framedConnection.setCipher(
                new DefaultClientCipher(handshakeService.getCrypto())
        );

        return framedConnection;
    }



    public FramedChatConnection framed() {
        return framedConnection;
    }

    @Override
    public void close() {
        try {
            if (framedConnection != null) {
                framedConnection.close();
            }
        } catch (Exception ignored) {}
    }
}
