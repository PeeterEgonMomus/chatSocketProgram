package org.example.chat.security;

public interface EncryptionStrategy {
    String encrypt(String plainText);
    String decrypt(String cipherText);
}
