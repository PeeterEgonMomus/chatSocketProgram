package org.example.chat.files;

import java.io.*;


/**
 * Design choice:
 * Serialization utility for FILE_CHUNK payloads.
 *
 * Encodes:
 * - transferId (UTF)
 * - chunk index (int)
 * - chunk length (int)
 * - raw data bytes
 *
 * ---------------------------------------------------------
 * Architectural Role
 * ---------------------------------------------------------
 *
 * Pure transport codec.
 *
 * It contains:
 * - No business logic
 * - No validation
 * - No encryption
 *
 * It simply defines the wire format.
 *
 * ---------------------------------------------------------
 * Why Separate Codec?
 * ---------------------------------------------------------
 *
 * - Keeps transfer logic clean
 * - Centralizes payload format
 * - Avoids duplication
 */
public final class ChunkCodec {

    private ChunkCodec() {}

    public static byte[] encode(String transferId, int index, byte[] data, int len)
            throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeUTF(transferId);
        out.writeInt(index);
        out.writeInt(len);
        out.write(data, 0, len);

        return baos.toByteArray();
    }
}
