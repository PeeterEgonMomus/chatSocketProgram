package org.example.chat.Client.handler;

import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;

import java.nio.charset.StandardCharsets;


/**
 * Handles incoming CHAT frames.
 *
 * Responsibilities:
 * - Validate frame type
 * - Decode payload as UTF-8 text
 * - Output chat message to console
 *
 * Important:
 * - Assumes payload is already plaintext
 * - Does not perform authentication or parsing
 *
 * Design:
 * - Single Responsibility: handle CHAT only
 * - Fails fast if wrong FrameType is received
 */
public final class ChatFrameHandler implements FrameHandler {

    @Override
    public void handle(Frame frame) {
        if (frame.getType() != FrameType.CHAT) {
            throw new IllegalArgumentException("Invalid frame type for ChatFrameHandler");
        }

        // Payload is already plaintext here
        String message = new String(frame.getPayload(), StandardCharsets.UTF_8);
        System.out.println(message);
    }
}
