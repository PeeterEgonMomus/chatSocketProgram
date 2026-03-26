package org.example.chat.handshake;

import org.example.chat.ClientHandler;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;

import java.nio.charset.StandardCharsets;

/**
 * RSA-based handshake implementation.
 *
 * Flow:
 * 1. Server sends its RSA public key to the client.
 * 2. Client generates a random AES session key.
 * 3. Client encrypts AES key with server RSA public key.
 * 4. Client sends encrypted AES key to server.
 * 5. Server decrypts and stores AES key for that client.
 * 6. Server confirms handshake completion.
 *
 * After this process:
 * - All further communication uses AES (symmetric encryption).
 * - RSA is only used during the handshake phase.
 *
 * Design Patterns Used:
 *
 * - Strategy Pattern:
 *   This is a concrete strategy implementing HandshakeService.
 *
 * - Template-like Protocol Flow:
 *   The method defines a fixed handshake protocol state machine.
 *
 * - Separation of Concerns:
 *   Handshake logic is isolated from encryption logic.
 */
public class RSAHandshakeService implements HandshakeService {

    /**
     * Executes the RSA + AES hybrid handshake.
     *
     * This method blocks until:
     * - AES key is successfully registered, or
     * - Handshake fails / connection closes.
     */
    @Override
    public void performHandshake(ClientHandler client) throws Exception {

        // Step 1: Send server's RSA public key
        sendServerPublicKey(client);

        boolean aesReady = false;

        // Step 2: Wait for client to send required handshake frames
        while (!aesReady) {

            Frame frame = client.readFrame();

            if (frame == null)
                throw new Exception("Handshake aborted");

            switch (frame.getType()) {

                /**
                 * Optional step:
                 * Client may send its own public key.
                 * Currently ignored in this implementation.
                 */
                case HANDSHAKE_CLIENT_KEY ->
                        Logger.debug("Client public key received (ignored)");

                /**
                 * Critical step:
                 * Client sends AES key encrypted with server RSA public key.
                 */
                case HANDSHAKE_AES_KEY -> {

                    // Register client's AES session key
                    client.encryption().registerClientAESKey(
                            client,
                            new String(frame.getPayload(), StandardCharsets.UTF_8)
                    );

                    // Confirm successful handshake
                    client.sendFrame(
                            new Frame(FrameType.HANDSHAKE_OK, new byte[0])
                    );

                    aesReady = true;

                    Logger.info("Handshake completed for client " + client);
                }

                /**
                 * Any unexpected frame during handshake
                 * is considered a protocol violation.
                 */
                default -> throw new IllegalStateException("Invalid handshake frame");
            }
        }
    }

    /**
     * Sends the server's RSA public key to the client.
     *
     * The key is Base64-encoded so it can be safely
     * transmitted inside a text-based frame.
     *
     * This is the first step of the handshake protocol.
     */
    private void sendServerPublicKey(ClientHandler client) throws Exception {

        client.sendFrame(
                new Frame(
                        FrameType.HANDSHAKE_SERVER_KEY,
                        client.encryption()
                                .getServerPublicKeyBase64()
                                .getBytes(StandardCharsets.UTF_8)
                )
        );
    }
}