package org.example.chat.security;

import org.example.chat.ClientHandler;
import org.example.chat.util.Logger;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HybridEncryption implements EncryptionStrategy {
    private final RSAEncryption rsa;
    private final Map<ClientHandler, AESEncryption> clientAES = new ConcurrentHashMap<>();

    public HybridEncryption(RSAEncryption rsa) {
        this.rsa = rsa;
    }

    /** Called when a client sends an AES key encrypted with server RSA public key. */
    public void registerClientAESKey(ClientHandler client, String encryptedAESKeyBase64) {
        try {
            // RSA decrypt -> raw AES bytes, then Base64 encode for AESEncryption
            byte[] aesBytes = rsa.decryptToBytes(encryptedAESKeyBase64);
            String aesKeyBase64 = Base64.getEncoder().encodeToString(aesBytes);
            clientAES.put(client, new AESEncryption(aesKeyBase64));
            Logger.info("AES session key registered for client " + client);
        } catch (RuntimeException e) {
            Logger.error("Failed to register AES key for " + client, e);
            throw e;
        }
    }



    public boolean hasAES(ClientHandler client) {
        return clientAES.containsKey(client);
    }

    public String getServerPublicKeyBase64() {
        return rsa.getPublicKeyBase64();
    }

    // Encrypt a message for a specific client (AES preferred)
    public String encryptForClient(ClientHandler client, String message, String clientPublicKeyBase64) {
        if (hasAES(client)) {
            return clientAES.get(client).encrypt(message);
        }
        return rsa.encryptWithPublicKey(message, clientPublicKeyBase64);
    }

    // Decrypt a message from a specific client (AES preferred)
    public String decryptFromClient(ClientHandler client, String payload) {
        if (hasAES(client)) {
            return clientAES.get(client).decrypt(payload);
        }
        return rsa.decrypt(payload);
    }

    public String encryptWithClientPublicKey(String message, String clientPublicKeyBase64) {
        return rsa.encryptWithPublicKey(message, clientPublicKeyBase64);
    }


    @Override
    public String encrypt(String plainText) {
        return rsa.encrypt(plainText);
    }

    @Override
    public String decrypt(String cipherText) {
        return rsa.decrypt(cipherText);
    }

    public void removeClient(ClientHandler client) {
        clientAES.remove(client);
    }
}
