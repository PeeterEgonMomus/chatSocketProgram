package org.example.chat.security;

import org.example.chat.ClientHandler;

public class EncryptionService {
    private final HybridEncryption hybrid;

    public EncryptionService(HybridEncryption hybrid) {
        this.hybrid = hybrid;
    }

    public void registerClientAESKey(ClientHandler client, String encryptedAESKeyBase64) {
        hybrid.registerClientAESKey(client, encryptedAESKeyBase64);
    }

    public String encryptForClient(ClientHandler client, String message, String clientPublicKeyBase64) {
        return hybrid.encryptForClient(client, message, clientPublicKeyBase64);
    }

    public String decryptFromClient(ClientHandler client, String payload) {
        return hybrid.decryptFromClient(client, payload);
    }

    public String getServerPublicKeyBase64() {
        return hybrid.getServerPublicKeyBase64();
    }

    public void removeClient(ClientHandler client) {
        hybrid.removeClient(client);
    }
}
