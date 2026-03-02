package org.example.chat.Client.protocol;

import org.example.chat.Client.handler.FrameHandler;
import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;

import java.util.EnumMap;
import java.util.Map;

public final class FrameDispatcher {

    private final Map<FrameType, FrameHandler> handlers = new EnumMap<>(FrameType.class);
    private boolean sealed = false;


    // Register a handler for a frame type
    public void register(FrameType type, FrameHandler handler) {
        if (sealed) {
            throw new IllegalStateException("Dispatcher already sealed");
        }
        handlers.put(type, handler);
    }


    // Dispatch a frame to its handler
    public void dispatch(Frame frame) throws Exception {
        FrameHandler handler = handlers.get(frame.getType());
        if (handler == null) {
            Logger.error("Unhandled frame type: " + frame.getType());
            return;
        }
        handler.handle(frame);
    }

    // Minimal start method to satisfy ClientRuntime
    public void start(FramedChatConnection connection) {
        // no-op for now
    }

    // Minimal stop method to satisfy ClientRuntime
    public void stop() {
        // no-op for now
    }
}
