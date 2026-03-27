package org.example.chat.Client.connection;

import org.example.chat.Client.protocol.HandshakeService;

import javax.crypto.SecretKey;
import java.net.Socket;

/**
 * Client Connection Orchestrator.
 *
 * Responsibility:
 * - Establish TCP connection
 * - Perform handshake
 * - Provide configured FramedChatConnection
 *
 * Architecture Role:
 * This is part of the Client Bootstrap / Infrastructure Layer.
 *
 * It coordinates:
 *      • Socket creation
 *      • Handshake negotiation
 *      • Initial connection lifecycle
 *
 * Important Design Decision:
 * - Handshake is performed in plaintext mode.
 * - Cipher is NOT installed here.
 *
 * Why?
 * Because:
 * - AES session key must first be negotiated.
 * - ClientRuntime installs the cipher afterwards.
 *
 * This keeps:
 * - Transport setup
 * - Crypto activation
 * - Runtime wiring
 *
 * Cleanly separated.
 *
 * Design Pattern:
 * - Orchestrator
 * - Infrastructure coordinator
 *
 * This class does NOT:
 * - Process frames
 * - Handle user input
 * - Manage runtime loop
 */
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

    /**
     * Establishes TCP connection and performs handshake.
     *
     * Flow:
     * 1. Open socket
     * 2. Create FramedChatConnection (plaintext)
     * 3. Perform RSA/AES handshake
     * 4. Return connection
     *
     * Cipher installation happens later in ClientRuntime.
     */
    public FramedChatConnection connect() throws Exception {
        socket = new Socket(host, port);
        framedConnection = new FramedChatConnection(socket);

        // Plaintext handshake
        handshakeService.perform(framedConnection);

        return framedConnection;
    }

    /**
     * Returns negotiated AES session key.
     *
     * Used by ClientRuntime to install cipher.
     */
    public SecretKey getSessionAESKey() {
        return handshakeService.getSessionAESKey();
    }

    /**
     * Gracefully closes connection.
     */
    @Override
    public void close() {
        try {
            if (framedConnection != null)
                framedConnection.close();
        } catch (Exception ignored) {}
    }
}