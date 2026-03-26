package org.example.chat;

import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameContext;
import org.example.chat.protocol.FrameType;

import java.util.HashMap;
import java.util.Map;

/**
 * Design choice:
 * Central protocol dispatcher.
 *
 * FrameRouter maps FrameType → FrameHandler
 * and delegates execution to the correct handler.
 *
 * ---------------------------------------------------------
 * Architectural Role
 * ---------------------------------------------------------
 *
 * This class represents the Protocol Dispatch Layer.
 *
 * It separates:
 *
 *   Transport Layer (Frame decoding)
 *   from
 *   Application Logic (Handlers)
 *
 * The router itself:
 * - Does NOT interpret payload
 * - Does NOT execute business logic
 * - Does NOT decrypt
 *
 * It only selects the correct handler.
 *
 * ---------------------------------------------------------
 * Why This Is Important
 * ---------------------------------------------------------
 *
 * Without this router, you would have:
 *
 *   if (type == X) { ... }
 *   else if (type == Y) { ... }
 *
 * That approach:
 * - Violates Open/Closed Principle
 * - Grows into a massive switch statement
 *
 * Instead, this design allows:
 *
 * - Adding new FrameTypes without modifying this class
 * - Independent handler implementations
 * - Clean extensibility
 *
 * ---------------------------------------------------------
 * Flow:
 * ---------------------------------------------------------
 *
 * ClientHandler receives Frame
 *        ↓
 * FrameRouter.route(...)
 *        ↓
 * FrameContext created
 *        ↓
 * Handler.handle(ctx)
 *
 * ---------------------------------------------------------
 * Important Design Decision:
 * ---------------------------------------------------------
 *
 * FrameContext is created here,
 * ensuring every handler receives:
 * - client
 * - server
 * - decrypted access
 * - send helpers
 *
 * This standardizes handler execution.
 */
public class FrameRouter {

    private final Map<FrameType, FrameHandler> handlers = new HashMap<>();

    public void register(FrameHandler handler) {
        handlers.put(handler.type(), handler);
    }

    /**
     * Routes a frame to its corresponding handler.
     *
     * Fail-fast design:
     * - If no handler is registered → throw exception.
     *
     * This ensures protocol mismatches are detected immediately
     * instead of silently ignored.
     *
     * FrameContext acts as an execution wrapper
     * providing:
     * - Decryption access
     * - Input stream helpers
     * - Send helpers
     */
    public void route(ClientHandler client, Frame frame) throws Exception {

        FrameHandler handler = handlers.get(frame.getType());

        if (handler == null)
            throw new IllegalStateException("No handler for " + frame.getType());

        FrameContext ctx = new FrameContext(client, frame);

        handler.handle(ctx);
    }
}