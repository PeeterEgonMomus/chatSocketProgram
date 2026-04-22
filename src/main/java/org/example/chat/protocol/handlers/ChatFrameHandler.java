package org.example.chat.protocol.handlers;

import org.example.chat.CommandRegistry;
import org.example.chat.FrameHandler;
import org.example.chat.protocol.FrameContext;
import org.example.chat.protocol.FrameType;
import java.nio.charset.StandardCharsets;

/**
 * Handles CHAT frames.
 *
 * Responsibility:
 * - Convert raw payload bytes into a UTF-8 message string
 * - Delegate command execution to the CommandRegistry
 *
 * Architecture:
 * - This class acts as a bridge between the transport layer (Frame)
 *   and the command execution layer.
 *
 * Important:
 * - It does NOT interpret commands itself.
 * - It does NOT contain business logic.
 *
 * Design Pattern:
 * - Adapter (Frame → Command input)
 * - Strategy (registered in FrameRouter by FrameType)
 *
 * Separation of Concerns:
 * Transport → Protocol → Command Layer
 */
public class ChatFrameHandler implements FrameHandler {

    private final CommandRegistry registry;

    public ChatFrameHandler(CommandRegistry registry) {
        this.registry = registry;
    }

    /**
     * Declares which frame type this handler is responsible for.
     *
     * This is used by FrameRouter for dynamic dispatch.
     */
    @Override
    public FrameType type() {
        return FrameType.CHAT;
    }

    /**
     * Converts the incoming payload into a string command
     * and delegates execution to the CommandRegistry.
     */
    @Override
    public void handle(FrameContext ctx) throws Exception {

        String message = new String(ctx.payload(), StandardCharsets.UTF_8);

        registry.executeCommand(ctx.client(), message);
    }
}