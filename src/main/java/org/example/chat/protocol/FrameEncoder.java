package org.example.chat.protocol;

import org.example.chat.util.Logger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Design choice:
 * Responsible for serializing Frame objects
 * into raw bytes for network transmission.
 *
 * Responsibilities:
 * - Write type byte
 * - Write payload length
 * - Write payload
 * - Flush output
 *
 * It does NOT:
 * - Encrypt data
 * - Modify payload
 *
 * Encryption occurs before frame creation.
 *
 * This strict separation ensures:
 * - Encoder remains simple
 * - Encryption logic stays centralized
 * - Transport format is consistent
 *
 * Logging provides visibility for debugging.
 */
public final class FrameEncoder {

    private FrameEncoder() {}

    public static void write(Frame frame, OutputStream out) throws IOException {
        DataOutputStream dataOut = new DataOutputStream(out);

        // 1 byte type
        dataOut.writeByte(frame.getType().getId());

        // 4 byte length
        dataOut.writeInt(frame.getLength());

        // payload
        dataOut.write(frame.getPayload());
        dataOut.flush();

        // Log frame info with payload preview (first 20 chars)
        Logger.debug("[FRAME →] type=" + frame.getType() +
                ", length=" + frame.getLength() +
                ", payload preview=" + preview(frame.getPayload()));
    }

    private static String preview(byte[] payload) {
        if (payload == null || payload.length == 0) return "<empty>";
        int len = Math.min(20, payload.length);
        return new String(payload, 0, len) + (payload.length > 20 ? "..." : "");
    }
}
