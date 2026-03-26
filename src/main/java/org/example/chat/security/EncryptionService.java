package org.example.chat.security;

import org.example.chat.ClientHandler;
import org.example.chat.protocol.FrameType;


/**
 * Design choice:
 * High-level encryption facade for the server.
 *
 * Responsibilities:
 * - Expose handshake-related encryption operations
 * - Expose session encryption/decryption operations
 * - Hide internal hybrid encryption complexity
 *
 * It does NOT:
 * - Implement AES directly
 * - Implement RSA directly
 * - Store encryption keys itself
 *
 * Architectural Role:
 * - Boundary between protocol layer and cryptographic layer
 * - Provides clean API for ClientHandler and FrameContext
 *
 * Structure:
 * - Delegates all logic to HybridEncryption
 *
 * Benefit:
 * - Protocol code never depends on concrete crypto implementations
 * - Enables future replacement of encryption model
 */
public class EncryptionService {

    private final HybridEncryption hybrid;

    public EncryptionService(HybridEncryption hybrid) {
        this.hybrid = hybrid;
    }

    /* HANDSHAKE */

    public void registerClientAESKey(ClientHandler client, String encryptedAESKeyBase64) {
        hybrid.registerClientAESKey(client, encryptedAESKeyBase64);
    }

    public String getServerPublicKeyBase64() {
        return hybrid.getServerPublicKeyBase64();
    }

    public void removeClient(ClientHandler client) {
        hybrid.removeClient(client);
    }

    /* SESSION */

    public byte[] encryptBytesForClient(ClientHandler client, FrameType type, byte[] payload) {
        return hybrid.encryptBytesForClient(client, type, payload);
    }

    public byte[] decryptBytesFromClient(ClientHandler client, FrameType type, byte[] payload) {
        return hybrid.decryptBytesFromClient(client, type, payload);
    }
}
