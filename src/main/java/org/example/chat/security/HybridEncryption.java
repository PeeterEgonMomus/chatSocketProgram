package org.example.chat.security;

import org.example.chat.ClientHandler;
import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HybridEncryption {

    private final RSAEncryption rsa;
    private final Map<ClientHandler, AESEncryption> sessions = new ConcurrentHashMap<>();

    public HybridEncryption(RSAEncryption rsa) {
        this.rsa = rsa;
    }

    /* ================= HANDSHAKE ================= */

    public String getServerPublicKeyBase64() {
        return rsa.getPublicKeyBase64();
    }

    public void registerClientAESKey(ClientHandler client, String encryptedAESKeyBase64) {
        try {
            byte[] aesBytes = rsa.decryptToBytes(encryptedAESKeyBase64);
            String aesKeyBase64 = Base64.getEncoder().encodeToString(aesBytes);
            sessions.put(client, new AESEncryption(aesKeyBase64));
            Logger.info("AES session established for client " + client);
        } catch (RuntimeException e) {
            Logger.error("Failed to register AES key for " + client, e);
            throw e;
        }
    }

    /* ================= SESSION (AES ONLY) ================= */

    private AESEncryption getSession(ClientHandler client) {
        AESEncryption aes = sessions.get(client);
        if (aes == null)
            throw new IllegalStateException("AES session not established for " + client);
        return aes;
    }

    public byte[] encryptBytesForClient(ClientHandler client, FrameType type, byte[] payload) {
        byte[] aad = type.name().getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = getSession(client).encryptBytes(payload, aad);
        Logger.debug("Encrypted " + type + " for " + client);
        return encrypted;
    }

    public byte[] decryptBytesFromClient(ClientHandler client, FrameType type, byte[] payload) {
        byte[] aad = type.name().getBytes(StandardCharsets.UTF_8);
        byte[] decrypted = getSession(client).decryptBytes(payload, aad);
        Logger.debug("Decrypted " + type + " from " + client);
        return decrypted;
    }

    public void removeClient(ClientHandler client) {
        sessions.remove(client);
        Logger.debug("Removed AES session for client " + client);
    }
}
