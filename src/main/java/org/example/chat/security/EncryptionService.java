package org.example.chat.security;

import org.example.chat.ClientHandler;
import org.example.chat.protocol.FrameType;

public class EncryptionService {

    private final HybridEncryption hybrid;

    public EncryptionService(HybridEncryption hybrid) {
        this.hybrid = hybrid;
    }

    /* HANDSHAKE */

    public void registerClientAESKey(ClientHandler client, String encryptedAESKeyBase64) {
        hybrid.registerClientAESKey(client, encryptedAESKeyBase64);
    }

    public String getServerPublicKeyBase64() {
        return hybrid.getServerPublicKeyBase64();
    }

    public void removeClient(ClientHandler client) {
        hybrid.removeClient(client);
    }

    /* SESSION */

    public byte[] encryptBytesForClient(ClientHandler client, FrameType type, byte[] payload) {
        return hybrid.encryptBytesForClient(client, type, payload);
    }

    public byte[] decryptBytesFromClient(ClientHandler client, FrameType type, byte[] payload) {
        return hybrid.decryptBytesFromClient(client, type, payload);
    }
}
