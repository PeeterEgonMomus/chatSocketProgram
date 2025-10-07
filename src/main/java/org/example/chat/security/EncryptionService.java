package org.example.chat.security;

import org.example.chat.ClientHandler;
import org.example.chat.util.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EncryptionService {
    private final RSAEncryption rsa;
    // map client identifier -> AES key (base64)
    private final Map<ClientHandler, String> aesKeys = new ConcurrentHashMap<>();

    public EncryptionService(RSAEncryption rsa) {
        this.rsa = rsa;
    }

    // --- RSA helpers (server private RSA decrypt)
    public String decryptWithServerPrivate(String cipherText) {
        return rsa.decrypt(cipherText);
    }

    public String getServerPublicKeyBase64() {
        return rsa.getPublicKeyBase64();
    }

    // RSA encrypt using arbitrary public key (server -> client backup)
    public String encryptWithPublicKey(String plain, String base64PublicKey) {
        return rsa.encryptWithPublicKey(plain, base64PublicKey);
    }

    // --- Hybrid (AES) management

    /** Register a client's AES key: client sent AES key encrypted with server RSA public key.
     *  encryptedAESKeyBase64 is RSA-encrypted value; after decrypting, store the resulting base64 AES key.
     */
    public void registerClientAESKey(ClientHandler client, String encryptedAESKeyBase64) {
        try {
            String aesKeyBase64 = rsa.decrypt(encryptedAESKeyBase64); // decrypt AES key with server private key
            aesKeys.put(client, aesKeyBase64);
            Logger.info("Registered AES session key for client " + client);
        } catch (RuntimeException e) {
            Logger.error("Failed to register client AES key", e);
            throw e;
        }
    }

    public boolean hasAESKey(ClientHandler client) {
        return aesKeys.containsKey(client);
    }

    /** Decrypt a message received from client. If client has AES key, use AES; else fallback to RSA decrypt. */
    public String decryptFromClient(ClientHandler client, String payload) {
        if (hasAESKey(client)) {
            String aesKeyBase64 = aesKeys.get(client);
            return AESEncryption.decryptWithKeyBase64(aesKeyBase64, payload);
        } else {
            // old behavior: clients might send RSA-encrypted messages (before AES handshake)
            return decryptWithServerPrivate(payload);
        }
    }

    /** Encrypt a plaintext for a client. If client has AES key, use AES (fast); else fallback to RSA encrypt with client's public key. */
    public String encryptForClient(ClientHandler client, String plaintext, String clientPublicKeyBase64) {
        if (hasAESKey(client)) {
            String aesKeyBase64 = aesKeys.get(client);
            return AESEncryption.encryptWithKeyBase64(aesKeyBase64, plaintext);
        } else {
            // fallback - client public key must be provided if AES not set
            if (clientPublicKeyBase64 == null) {
                throw new IllegalStateException("No AES key and no client public key available");
            }
            return encryptWithPublicKey(plaintext, clientPublicKeyBase64);
        }
    }

    /** Remove mapping when client disconnects */
    public void removeClient(ClientHandler client) {
        aesKeys.remove(client);
    }
}
