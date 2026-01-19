package org.example.chat.Client.protocol;

import org.example.chat.Client.crypto.ClientCrypto;
import org.example.chat.protocol.FrameType;

import java.nio.charset.StandardCharsets;

public final class DefaultClientCipher implements ClientCipher {

    private final ClientCrypto crypto;

    public DefaultClientCipher(ClientCrypto crypto) {
        this.crypto = crypto;
    }

    @Override
    public byte[] encrypt(FrameType type, byte[] payload) throws Exception {
        return switch (type) {

            // 🔐 Encrypted text frames
            case CHAT -> crypto.encryptForServer(
                    new String(payload, StandardCharsets.UTF_8)
            ).getBytes(StandardCharsets.UTF_8);

            // 🔐 Encrypted binary frames
            case FILE_CHUNK -> crypto.encryptBytesForServer(payload);

            // ❌ Plaintext protocol frames
            case FILE_META, FILE_END,
                 HANDSHAKE_CLIENT_KEY,
                 HANDSHAKE_AES_KEY -> payload;

            default -> payload;
        };
    }

    @Override
    public byte[] decrypt(FrameType type, byte[] payload) throws Exception {
        return switch (type) {

            case CHAT -> crypto.decryptFromServer(
                    new String(payload, StandardCharsets.UTF_8)
            ).getBytes(StandardCharsets.UTF_8);

            case FILE_CHUNK -> crypto.decryptBytesFromServer(payload);

            default -> payload;
        };
    }
}
