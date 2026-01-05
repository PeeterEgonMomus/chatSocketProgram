package org.example.chat.Client;

import org.example.chat.util.Logger;

import java.io.IOException;

public class HandshakeManager {

    private final ClientCrypto crypto;
    private final ChatConnection connection;

    public HandshakeManager(ClientCrypto crypto, ChatConnection connection) {
        this.crypto = crypto;
        this.connection = connection;
    }

    public boolean doClientHandshake() {
        try {
            // 1) receive server public key
            String firstLine = connection.receive();
            if (firstLine == null || !firstLine.startsWith("PUBLIC_KEY:")) {
                throw new IOException("Missing PUBLIC_KEY from server");
            }

            String serverPub = firstLine.substring("PUBLIC_KEY:".length()).trim();
            crypto.setServerPublicKeyBase64(serverPub);
            Logger.debug("Received server public key");

            // 2) send client public key
            connection.send("CLIENT_KEY:" + crypto.getPublicKeyBase64());
            Logger.debug("Sent client public key");

            // 3) generate + send AES key
            String aesKey = crypto.generateAESKeyBase64();
            String encryptedAES = crypto.encryptForServerRSA(aesKey);
            connection.send("AES_KEY:" + encryptedAES);
            Logger.debug("Sent AES key");

            // 4) wait for AES_OK (IMPORTANT)
            String ack = connection.receive();
            if (!"AES_OK".equals(ack)) {
                throw new IOException("Expected AES_OK, got: " + ack);
            }

            crypto.markAESReady();
            Logger.info("Handshake complete, AES ready");
            return true;

        } catch (Exception e) {
            Logger.error("Handshake failed", e);
            return false;
        }
    }
}
