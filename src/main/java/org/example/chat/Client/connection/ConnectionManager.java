package org.example.chat.Client.connection;

import org.example.chat.Client.protocol.HandshakeService;

import javax.crypto.SecretKey;
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

        // Perform plaintext handshake only
        handshakeService.perform(framedConnection);

        // ⚠️ DO NOT install cipher here
        // Cipher will be installed by ClientRuntime after AES is negotiated

        return framedConnection;
    }

    /**
     * Returns the AES session key from handshake
     */
    public SecretKey getSessionAESKey() {
        return handshakeService.getSessionAESKey();
    }

    @Override
    public void close() {
        try {
            if (framedConnection != null) framedConnection.close();
        } catch (Exception ignored) {}
    }
}
