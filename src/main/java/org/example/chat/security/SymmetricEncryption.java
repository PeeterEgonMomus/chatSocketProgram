package org.example.chat.security;

public interface SymmetricEncryption {

    byte[] encrypt(byte[] plainText, byte[] aad);

    byte[] decrypt(byte[] cipherText, byte[] aad);
}