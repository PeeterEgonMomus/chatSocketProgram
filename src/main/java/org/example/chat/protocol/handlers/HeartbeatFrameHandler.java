package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.protocol.FrameContext;
import org.example.chat.protocol.FrameType;

/**
 * Handles PING and PONG frames.
 *
 * Responsibilities:
 * - Update client heartbeat timestamp
 * - Reply to PING with PONG
 *
 * No domain logic.
 * Pure connection-level behavior.
 */
public class HeartbeatFrameHandler implements FrameHandler {

    @Override
    public FrameType type() {
        return FrameType.PING;
    }

    @Override
    public void handle(FrameContext ctx) throws Exception {

        // update heartbeat
        ctx.client().updateHeartbeat();

        // reply with PONG
        ctx.client().sendEncrypted(FrameType.PONG, new byte[0]);
    }
}