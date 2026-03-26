package org.example.chat.protocol;

import java.util.Arrays;

/**
 * Design choice:
 * Immutable representation of a protocol frame.
 *
 * A frame consists of:
 * - FrameType (1 byte)
 * - Payload length (4 bytes)
 * - Payload (byte[])
 *
 * This class is intentionally:
 * - Simple
 * - Immutable
 * - Transport-focused
 *
 * It contains no:
 * - Encryption logic
 * - Parsing logic
 * - Business meaning
 *
 * It is purely a protocol data structure.
 *
 * Immutability ensures:
 * - Thread safety
 * - Predictable routing
 * - No accidental mutation during handling
 */
public final class Frame {
    private final FrameType type;
    private final byte[] payload;

    public Frame(FrameType type, byte[] payload) {
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        if (payload == null) throw new IllegalArgumentException("payload cannot be null");

        this.type = type;
        this.payload = payload;
    }

    public FrameType getType() {
        return type;
    }

    public int getLength() {
        return payload.length;
    }

    public byte[] getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "Frame{" +
                "type=" + type +
                ", length=" + payload.length +
                '}';
    }
}
