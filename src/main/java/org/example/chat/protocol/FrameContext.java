package org.example.chat.protocol;

import org.example.chat.ClientHandler;
import org.example.chat.ChatServer;
import org.example.chat.security.EncryptionService;

import java.io.*;


/**
 * Design choice:
 * Encapsulates all contextual information required to process
 * a single incoming frame.
 *
 * This class acts as a per-frame execution context and provides:
 *
 * - Access to the client connection
 * - Access to the server
 * - Lazy payload decryption
 * - Lazy DataInputStream creation
 * - Simplified read helpers
 * - Encrypted response sending
 *
 * Architectural Role:
 * --------------------
 * FrameContext decouples FrameHandler implementations
 * from encryption and byte-level parsing concerns.
 *
 * Instead of each handler manually:
 * - Decrypting payload
 * - Creating streams
 * - Handling encryption details
 *
 * They simply call:
 *     ctx.readUTF()
 *
 * This centralizes cryptographic handling
 * and ensures consistency across all handlers.
 *
 * Lazy Decryption:
 * ----------------
 * Payload decryption occurs only once and only when needed.
 * The decrypted bytes are cached for reuse.
 *
 * This improves:
 * - Performance
 * - Clarity
 * - Safety
 *
 * Security Boundary:
 * -------------------
 * EncryptionService is accessed only through the server,
 * keeping cryptographic logic isolated from handlers.
 *
 * This class acts as a protective boundary between:
 *
 * Network → Encryption → FrameContext → Handler → Service
 *
 * Clean layering is preserved.
 */
public class FrameContext {

    private final ClientHandler client;
    private final Frame frame;
    private final EncryptionService encryption;

    private byte[] decrypted;
    private DataInputStream input;

    public FrameContext(
            ClientHandler client,
            Frame frame,
            EncryptionService encryption
    ) {
        this.client = client;
        this.frame = frame;
        this.encryption = encryption;
    }

    public ClientHandler client() {
        return client;
    }

    public Frame frame() {
        return frame;
    }

    public FrameType type() {
        return frame.getType();
    }

    /* ===============================
       Decryption helpers
       =============================== */

    public byte[] payload() throws Exception {

        if (decrypted == null) {

            decrypted =
                    encryption.decryptBytesFromClient(
                            client,
                            frame.getType(),
                            frame.getPayload()
                    );
        }

        return decrypted;
    }

    public DataInputStream input() throws Exception {

        if (input == null) {

            input = new DataInputStream(
                    new ByteArrayInputStream(payload())
            );
        }

        return input;
    }

    public String readUTF() throws Exception {
        return input().readUTF();
    }

    /* ===============================
       Send helpers
       =============================== */

    public void send(FrameType type, byte[] payload) throws Exception {
        client.sendEncrypted(type, payload);
    }

}