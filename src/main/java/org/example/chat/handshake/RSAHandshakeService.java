package org.example.chat.handshake;

import org.example.chat.ClientHandler;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;
import org.example.chat.security.EncryptionService;
import org.example.chat.util.Logger;

import java.nio.charset.StandardCharsets;

public class RSAHandshakeService implements HandshakeService {

    private final EncryptionService encryption;

    public RSAHandshakeService(EncryptionService encryption) {
        this.encryption = encryption;
    }

    @Override
    public void performHandshake(ClientHandler client) throws Exception {

        sendServerPublicKey(client);

        boolean aesReady = false;

        while (!aesReady) {

            Frame frame = client.readFrame();

            if (frame == null)
                throw new Exception("Handshake aborted");

            switch (frame.getType()) {

                case HANDSHAKE_CLIENT_KEY ->
                        Logger.debug("Client public key received (ignored)");

                case HANDSHAKE_AES_KEY -> {

                    encryption.registerClientAESKey(
                            client,
                            new String(frame.getPayload(), StandardCharsets.UTF_8)
                    );

                    client.sendFrame(
                            new Frame(FrameType.HANDSHAKE_OK, new byte[0])
                    );

                    aesReady = true;

                    Logger.info("Handshake completed for client " + client);
                }

                default -> throw new IllegalStateException("Invalid handshake frame");
            }
        }
    }

    private void sendServerPublicKey(ClientHandler client) throws Exception {

        client.sendFrame(
                new Frame(
                        FrameType.HANDSHAKE_SERVER_KEY,
                        encryption.getServerPublicKeyBase64()
                                .getBytes(StandardCharsets.UTF_8)
                )
        );
    }
}