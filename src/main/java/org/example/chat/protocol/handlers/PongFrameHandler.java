package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.protocol.FrameContext;
import org.example.chat.protocol.FrameType;

public class PongFrameHandler implements FrameHandler {

    @Override
    public FrameType type() {
        return FrameType.PONG;
    }

    @Override
    public void handle(FrameContext ctx) {
        ctx.client().updateHeartbeat();
    }
}