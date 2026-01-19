package org.example.chat.Client.protocol;

import org.example.chat.Client.connection.FramedConnection;
import org.example.chat.Client.crypto.ClientCrypto;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;

import java.nio.charset.StandardCharsets;

public final class HandshakeService {

    private final ClientCrypto crypto;

    public HandshakeService(ClientCrypto crypto) {
        this.crypto = crypto;
    }

    public void perform(FramedConnection connection) throws Exception {

        Frame serverKeyFrame = connection.receive();
        if (serverKeyFrame.getType() != FrameType.HANDSHAKE_SERVER_KEY) {
            throw new IllegalStateException(
                    "Expected HANDSHAKE_SERVER_KEY, got " + serverKeyFrame.getType()
            );
        }

        String serverPubKey =
                new String(serverKeyFrame.getPayload(), StandardCharsets.UTF_8);
        crypto.setServerPublicKeyBase64(serverPubKey);

        connection.send(new Frame(
                FrameType.HANDSHAKE_CLIENT_KEY,
                crypto.getPublicKeyBase64().getBytes(StandardCharsets.UTF_8)
        ));

        String aesKeyBase64 = crypto.generateAESKeyBase64();
        String encryptedAES = crypto.encryptForServerRSA(aesKeyBase64);

        connection.send(new Frame(
                FrameType.HANDSHAKE_AES_KEY,
                encryptedAES.getBytes(StandardCharsets.UTF_8)
        ));

        Frame ok = connection.receive();
        if (ok.getType() != FrameType.HANDSHAKE_OK) {
            throw new IllegalStateException(
                    "Expected HANDSHAKE_OK, got " + ok.getType()
            );
        }

        crypto.markAESReady();
    }

    public ClientCrypto getCrypto() {
        return crypto;
    }
}
