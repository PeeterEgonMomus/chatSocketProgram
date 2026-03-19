package org.example.chat;

import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameContext;
import org.example.chat.protocol.FrameType;

import java.util.HashMap;
import java.util.Map;

public class FrameRouter {

    private final Map<FrameType, FrameHandler> handlers = new HashMap<>();

    public void register(FrameHandler handler) {
        handlers.put(handler.type(), handler);
    }

    public void route(ClientHandler client, Frame frame) throws Exception {

        FrameHandler handler = handlers.get(frame.getType());

        if (handler == null)
            throw new IllegalStateException("No handler for " + frame.getType());

        FrameContext ctx = new FrameContext(client, frame);

        handler.handle(ctx);
    }
}