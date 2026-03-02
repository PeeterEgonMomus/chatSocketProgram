package org.example.chat.Client.protocol;

import org.example.chat.Client.connection.FramedConnection;
import org.example.chat.Client.crypto.ClientHandshakeCrypto;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class HandshakeService {

    private final ClientHandshakeCrypto crypto;

    private SecretKey sessionAESKey;

    public HandshakeService(ClientHandshakeCrypto crypto) {
        this.crypto = crypto;
    }

    public void perform(FramedConnection connection) throws Exception {

        // 1️⃣ Receive server public key
        Frame serverKeyFrame = connection.receive();
        if (serverKeyFrame.getType() != FrameType.HANDSHAKE_SERVER_KEY) {
            throw new IllegalStateException(
                    "Expected HANDSHAKE_SERVER_KEY, got " + serverKeyFrame.getType()
            );
        }

        String serverPubKey =
                new String(serverKeyFrame.getPayload(), StandardCharsets.UTF_8);

        crypto.setServerPublicKeyBase64(serverPubKey);

        // 2️⃣ Send client public key
        connection.send(new Frame(
                FrameType.HANDSHAKE_CLIENT_KEY,
                crypto.getClientPublicKeyBase64()
                        .getBytes(StandardCharsets.UTF_8)
        ));

        // 3️⃣ Generate AES key (base64)
        String aesKeyBase64 = crypto.generateAESKeyBase64();

        // Convert to SecretKey and store locally
        byte[] decoded = Base64.getDecoder().decode(aesKeyBase64);
        sessionAESKey = new SecretKeySpec(decoded, "AES");

        // 4️⃣ Encrypt AES key with server RSA
        String encryptedAES = crypto.encryptAESKeyForServer(aesKeyBase64);

        connection.send(new Frame(
                FrameType.HANDSHAKE_AES_KEY,
                encryptedAES.getBytes(StandardCharsets.UTF_8)
        ));

        // 5️⃣ Wait for OK
        Frame ok = connection.receive();
        if (ok.getType() != FrameType.HANDSHAKE_OK) {
            throw new IllegalStateException(
                    "Expected HANDSHAKE_OK, got " + ok.getType()
            );
        }
    }

    public SecretKey getSessionAESKey() {
        if (sessionAESKey == null) {
            throw new IllegalStateException("Handshake not completed yet");
        }
        return sessionAESKey;
    }
}
