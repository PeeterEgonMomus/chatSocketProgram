package org.example.chat.protocol;

public enum FrameType {
    HANDSHAKE_SERVER_KEY(10),
    HANDSHAKE_CLIENT_KEY(11),
    HANDSHAKE_AES_KEY(12),
    HANDSHAKE_OK(13),

    CHAT(1),
    FILE_META(2),
    FILE_CHUNK(3),
    FILE_END(4),
    FILE_ACCEPT(5),
    ERROR(6),
    FILE_REJECT(7);

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

