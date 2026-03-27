package org.example.chat.Client.crypto;

import org.example.chat.protocol.FrameType;

/**
 * ClientCrypto defines the contract for encryption/decryption
 * at the client-side transport/protocol layer.
 *
 * Responsibilities:
 * - Encrypt data before sending to the server
 * - Decrypt data received from the server
 * - Report readiness of AES session key
 *
 * Architecture Role:
 * - Encryption abstraction for ClientRuntime
 * - Used by FramedChatConnection to secure payloads
 */
public interface ClientCrypto {

    /**
     * Encrypts payload for the server, using the appropriate cipher
     * and potentially including type-based AAD (additional authenticated data)
     */
    byte[] encryptBytesForServer(byte[] payload, FrameType type) throws Exception;

    /**
     * Decrypts payload received from the server
     */
    byte[] decryptBytesFromServer(byte[] payload, FrameType type) throws Exception;

    /**
     * Returns true if AES key has been established and encryption is ready
     */
    boolean isAESReady();
}