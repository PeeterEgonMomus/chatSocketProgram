package org.example.chat;

import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameContext;
import org.example.chat.protocol.FrameType;

/**
 * Design choice:
 * Strategy Pattern for protocol handling.
 *
 * Each FrameHandler:
 * - Declares which FrameType it supports
 * - Implements how that frame is processed
 *
 * This eliminates:
 * - Switch statements
 * - Conditional dispatch logic
 *
 * ---------------------------------------------------------
 * Architectural Role:
 * ---------------------------------------------------------
 *
 * FrameHandler is the boundary between:
 *
 *   Protocol Layer
 *   and
 *   Application / Domain Layer
 *
 * Handlers:
 * - Parse payload
 * - Call services
 * - Never contain domain state
 *
 * They are stateless adapters.
 *
 * ---------------------------------------------------------
 * Why an Interface?
 * ---------------------------------------------------------
 *
 * Enables:
 * - Polymorphic dispatch
 * - Test mocking
 * - Plugin-style handler registration
 *
 * This is textbook Strategy Pattern usage.
 */
public interface FrameHandler {

    FrameType type();

    void handle(FrameContext ctx) throws Exception;

}