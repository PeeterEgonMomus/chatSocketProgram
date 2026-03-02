package org.example.chat.Client.protocol;

import org.example.chat.Client.crypto.ClientCrypto;
import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;

public final class DefaultClientCipher implements ClientCipher {

    private final ClientCrypto crypto;

    public DefaultClientCipher(ClientCrypto crypto) {
        this.crypto = crypto;
    }

    private boolean isHandshakeFrame(FrameType type) {
        return switch (type) {
            case HANDSHAKE_CLIENT_KEY,
                 HANDSHAKE_AES_KEY,
                 HANDSHAKE_OK,
                 HANDSHAKE_SERVER_KEY -> true;
            default -> false;
        };
    }

    @Override
    public byte[] encrypt(FrameType type, byte[] payload) throws Exception {

        Logger.debug("Cipher.encrypt | type=" + type +
                " | payload length=" + payload.length);

        if (isHandshakeFrame(type)) {
            Logger.debug("Cipher.encrypt | Handshake frame (plaintext)");
            return payload;
        }

        if (!crypto.isAESReady()) {
            throw new IllegalStateException(
                    "Attempted to encrypt secure frame before AES ready: " + type
            );
        }

        Logger.debug("Cipher.encrypt | AES+AAD secure mode for " + type);

        return crypto.encryptBytesForServer(payload, type);
    }

    @Override
    public byte[] decrypt(FrameType type, byte[] payload) throws Exception {

        Logger.debug("Cipher.decrypt | type=" + type +
                " | payload length=" + payload.length);

        if (isHandshakeFrame(type)) {
            Logger.debug("Cipher.decrypt | Handshake frame (plaintext)");
            return payload;
        }

        if (!crypto.isAESReady()) {
            throw new IllegalStateException(
                    "Attempted to decrypt secure frame before AES ready: " + type
            );
        }

        Logger.debug("Cipher.decrypt | AES+AAD secure mode for " + type);

        return crypto.decryptBytesFromServer(payload, type);
    }
}
