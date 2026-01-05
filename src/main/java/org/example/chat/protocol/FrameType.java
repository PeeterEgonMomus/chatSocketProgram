package org.example.chat.protocol;

public enum FrameType {
    CHAT(1),
    FILE_META(2),
    FILE_CHUNK(3),
    FILE_END(4),
    ERROR(5);

    private final byte id;

    FrameType(int id) {
        this.id = (byte) id;
    }

    public byte getId() {
        return id;
    }

    public static FrameType fromId(byte id) {
        for (FrameType t : values()) {
            if (t.id == id) return t;
        }
        throw new IllegalArgumentException("Unknown frame type id: " + id);
    }
}
