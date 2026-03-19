package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.protocol.FrameContext;
import org.example.chat.protocol.FrameType;
import java.nio.charset.StandardCharsets;

public class ChatFrameHandler implements FrameHandler {

    @Override
    public FrameType type() {
        return FrameType.CHAT;
    }

    @Override
    public void handle(FrameContext ctx) throws Exception {
        String message = new String(ctx.payload(), StandardCharsets.UTF_8);
        ctx.server()
                .getRegistry()
                .executeCommand(ctx.client(), message);
    }
}