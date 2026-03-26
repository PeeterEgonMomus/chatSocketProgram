package org.example.chat.security;


/**
 * Design choice:
 * Strategy abstraction for asymmetric encryption algorithms.
 *
 * Responsibilities:
 * - Define contract for encryption
 * - Define contract for decryption
 *
 * Intended Usage:
 * - Primarily used for RSA-based operations during handshake
 *
 * It does NOT:
 * - Represent session encryption (AES)
 * - Manage keys
 * - Handle client sessions
 *
 * Architectural Role:
 * - Enables interchangeable asymmetric algorithms
 * - Supports Strategy Pattern
 *
 * Current Implementation:
 * - RSAEncryption
 */
public interface EncryptionStrategy {
    // Server-side decrypt (ciphertext -> plaintext) using server private key
    String decrypt(String cipherText);

    // (Optional) server-side encrypt using server key (not used for server->client)
    String encrypt(String plainText);
}
