package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.protocol.FrameContext;
import org.example.chat.protocol.FrameType;

public class PingFrameHandler implements FrameHandler {

    @Override
    public FrameType type() {
        return FrameType.PING;
    }

    @Override
    public void handle(FrameContext ctx) throws Exception {
        ctx.client().sendEncrypted(FrameType.PONG, new byte[0]);
    }
}