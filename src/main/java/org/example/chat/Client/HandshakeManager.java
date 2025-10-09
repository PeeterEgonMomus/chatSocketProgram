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

    /**
     * Perform the handshake steps synchronously:
     * 1) read PUBLIC_KEY:...
     * 2) send CLIENT_KEY:...
     * 3) generate AES key and send AES_KEY: (RSA-encrypted)
     *
     * Returns true if handshake initiated (client side); false if failure.
     */
    public boolean doClientHandshake() {
        try {
            // Step 1: receive server public key
            String firstLine = connection.receive();
            if (firstLine == null || !firstLine.startsWith("PUBLIC_KEY:")) {
                Logger.error("Server did not send public key properly",
                        new RuntimeException("Missing PUBLIC_KEY"));
                return false;
            }

            String serverPub = firstLine.substring("PUBLIC_KEY:".length()).trim();
            crypto.setServerPublicKeyBase64(serverPub);
            Logger.debug("Received server public key (" + serverPub.length() + " chars)");

            // Step 2: send client public key
            String clientPub = crypto.getPublicKeyBase64();
            connection.send("CLIENT_KEY:" + clientPub);
            Logger.debug("Sent client public key to server");

            // Step 3: generate and send AES key
            String aesKeyBase64 = crypto.generateAESKeyBase64();
            Logger.debug("Generated AES key: " + (aesKeyBase64.length() > 20
                    ? aesKeyBase64.substring(0, 20) + "..."
                    : aesKeyBase64));

            String encryptedAESKey = crypto.encryptForServerRSA(aesKeyBase64);
            Logger.debug("Encrypted AES key length: " + encryptedAESKey.length());
            connection.send("AES_KEY:" + encryptedAESKey);
            Logger.debug("Sent AES key to server (RSA-encrypted)");

            return true;
        } catch (IOException e) {
            Logger.error("I/O during handshake failed", e);
            return false;
        } catch (Exception e) {
            Logger.error("Handshake failed", e);
            return false;
        }
    }
}
