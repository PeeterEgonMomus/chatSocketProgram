package org.example.chat.handshake;

import org.example.chat.ClientHandler;

/**
 * Contract for performing a cryptographic handshake
 * between the server and a connected client.
 *
 * The handshake is responsible for establishing
 * a secure session (e.g., negotiating encryption keys)
 * before normal encrypted communication begins.
 *
 * This abstraction allows multiple handshake
 * implementations (RSA-based, ECDH-based, etc.)
 * without changing the rest of the system.
 *
 * Design Pattern:
 * - Strategy Pattern
 *   Different handshake mechanisms can be swapped
 *   without modifying ClientHandler.
 */
public interface HandshakeService {

    /**
     * Executes the handshake protocol for a specific client.
     *
     * Responsibilities:
     * - Exchange necessary cryptographic material
     * - Validate handshake frames
     * - Establish a secure symmetric session key
     *
     * @param client The connected client
     * @throws Exception If the handshake fails or is aborted
     */
    void performHandshake(ClientHandler client) throws Exception;

}