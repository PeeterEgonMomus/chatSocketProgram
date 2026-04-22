package org.example.chat.Client.handler;

import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;

/**
 * Handles server heartbeat PING frames.
 *
 * Responsibility:
 * - Immediately respond with PONG
 *
 * Architectural Role:
 * - Frame handling layer
 * - System-level protocol reaction
 */
public final class PingFrameHandler implements FrameHandler {

    private final FramedChatConnection connection;

    public PingFrameHandler(FramedChatConnection connection) {
        this.connection = connection;
    }

    @Override
    public void handle(Frame frame) throws Exception {

        if (frame.getType() != FrameType.PING) {
            throw new IllegalArgumentException(
                    "Invalid frame type for PingFrameHandler"
            );
        }

        Logger.debug("PING received from server");

        Frame pong = new Frame(FrameType.PONG, new byte[0]);

        connection.send(pong);

        Logger.debug("PONG sent to server");
    }
}