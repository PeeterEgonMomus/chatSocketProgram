package org.example.chat.security;

public interface AsymmetricEncryption {

    byte[] decrypt(byte[] cipherText);

    byte[] encrypt(byte[] plainText);

    String getPublicKeyBase64();
}