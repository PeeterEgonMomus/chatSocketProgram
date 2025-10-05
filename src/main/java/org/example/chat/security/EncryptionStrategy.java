package org.example.chat.security;

public interface EncryptionStrategy {
    // Server-side decrypt (ciphertext -> plaintext) using server private key
    String decrypt(String cipherText);

    // (Optional) server-side encrypt using server key (not used for server->client)
    String encrypt(String plainText);
}
