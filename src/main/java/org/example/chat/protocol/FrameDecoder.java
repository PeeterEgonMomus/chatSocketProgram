package org.example.chat.protocol;

import org.example.chat.util.Logger;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class FrameDecoder {

    private FrameDecoder() {}

    public static Frame read(InputStream in) throws IOException {
        DataInputStream dataIn = new DataInputStream(in);

        // read type (blocks until 1 byte available)
        byte typeId;
        try {
            typeId = dataIn.readByte();
        } catch (IOException e) {
            return null; // clean EOF handling
        }

        FrameType type = FrameType.fromId(typeId);

        // read length
        int length = dataIn.readInt();
        if (length < 0 || length > 100_000_000) {
            throw new IOException("Invalid frame length: " + length);
        }

        // read payload fully
        byte[] payload = new byte[length];
        dataIn.readFully(payload);

        // Log frame info with payload preview (first 20 chars or bytes)
        Logger.debug("[FRAME ←] type=" + type +
                ", length=" + length +
                ", payload preview=" + preview(payload));

        return new Frame(type, payload);
    }

    private static String preview(byte[] payload) {
        if (payload == null || payload.length == 0) return "<empty>";
        int len = Math.min(20, payload.length);
        return new String(payload, 0, len) + (payload.length > 20 ? "..." : "");
    }
}
