package org.example.chat.protocol;

public enum FrameType {
    HANDSHAKE_SERVER_KEY(10),
    HANDSHAKE_CLIENT_KEY(11),
    HANDSHAKE_AES_KEY(12),
    HANDSHAKE_OK(13),

    CHAT(1),
    FILE_META(2), // LEGACY - do not use for new transfers
    FILE_ACCEPT(5),
    FILE_REJECT(7),

    // FILE NEGOTIATION
    FILE_OFFER(20),     // server → recipient
    FILE_OFFER_REPLY(21), // recipient → server

    // FILE TRANSPORT
    FILE_START(22),     // server → recipient
    FILE_CHUNK(3),
    FILE_END(4),

    // FILE NEGOTIATION (NEW)
    SEND_FILE_REQUEST(30),   // sender → server (routing request)
    SEND_FILE_READY(31),     // server → sender (recipient validated)


    ERROR(6);


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

