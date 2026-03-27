package org.example.chat.Client.protocol;

import org.example.chat.Client.handler.FrameHandler;
import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;

import java.util.EnumMap;
import java.util.Map;

/**
 * FrameDispatcher routes incoming frames to their respective handlers.
 *
 * Responsibilities:
 * - Maintain mapping of FrameType → FrameHandler
 * - Dispatch frames to correct handler
 * - Support start/stop lifecycle for ClientRuntime
 *
 * Architecture Role:
 * - Central event dispatch for all client-side frame handling
 * - Allows modular extension of new frame types (e.g., chat, file transfer)
 */
public final class FrameDispatcher {

    private final Map<FrameType, FrameHandler> handlers = new EnumMap<>(FrameType.class);
    private boolean sealed = false;

    public void register(FrameType type, FrameHandler handler) {
        if (sealed) {
            throw new IllegalStateException("Dispatcher already sealed");
        }
        handlers.put(type, handler);
    }

    public void dispatch(Frame frame) throws Exception {
        FrameHandler handler = handlers.get(frame.getType());
        if (handler == null) {
            Logger.error("Unhandled frame type: " + frame.getType());
            return;
        }
        handler.handle(frame);
    }

    public void start(FramedChatConnection connection) {
        // no-op for now; frame reader drives dispatching
    }

    public void stop() {
        // no-op for now
    }
}