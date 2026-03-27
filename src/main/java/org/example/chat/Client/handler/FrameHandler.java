package org.example.chat.Client.handler;

import org.example.chat.protocol.Frame;

/**
 * Contract for handling incoming protocol Frames.
 *
 * Each implementation is responsible for exactly one concern
 * or a specific set of FrameTypes.
 *
 * Design:
 * - Enables separation of responsibilities
 * - Allows handler-based dispatch architecture
 * - Supports Open/Closed Principle
 *
 * Usage:
 *   FrameDispatcher → selects handler → calls handle(frame)
 */
public interface FrameHandler {
    void handle(Frame frame) throws Exception;
}
