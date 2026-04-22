package org.example.chat.protocol;

import java.util.HashMap;
import java.util.Map;

public enum FrameType {
    HANDSHAKE_SERVER_KEY(10),
    HANDSHAKE_CLIENT_KEY(11),
    HANDSHAKE_AES_KEY(12),
    HANDSHAKE_OK(13),

    CHAT(1),
    FILE_META(2), // LEGACY - do not use for new transfers
    FILE_ACCEPT(5),
    FILE_REJECT(7),

    FILE_OFFER(20),
    FILE_OFFER_REPLY(21),

    FILE_START(22),
    FILE_CHUNK(3),
    FILE_END(4),

    SEND_FILE_REQUEST(30),
    SEND_FILE_READY(31),

    GAME_INVITE(40),
    GAME_ACCEPT(41),
    GAME_DECLINE(42),
    GAME_MOVE(43),
    GAME_CANCEL(44),

    PING(50),
    PONG(51),

    ERROR(6);

    private final byte id;

    private static final Map<Byte, FrameType> BY_ID = new HashMap<>();

    static {
        for (FrameType type : values()) {
            if (BY_ID.put(type.id, type) != null) {
                throw new IllegalStateException("Duplicate FrameType id: " + type.id);
            }
        }
    }

    FrameType(int id) {
        this.id = (byte) id;
    }

    public byte getId() {
        return id;
    }

    public static FrameType fromId(byte id) {
        FrameType type = BY_ID.get(id);
        if (type == null) {
            throw new IllegalArgumentException("Unknown frame type id: " + id);
        }
        return type;
    }
}