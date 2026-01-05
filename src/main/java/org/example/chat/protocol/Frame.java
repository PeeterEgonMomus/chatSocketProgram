package org.example.chat.protocol;

import java.util.Arrays;

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
