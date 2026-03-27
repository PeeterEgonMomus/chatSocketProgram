package org.example.chat.Client.gateway;

import org.example.chat.protocol.Frame;


/**
 * Abstraction for sending messages from the client.
 *
 * This interface decouples higher-level client logic
 * from the underlying transport implementation.
 *
 * Responsibilities:
 * - Send raw protocol Frames
 * - Provide convenience methods for common message types (e.g., chat)
 *
 * Design Purpose:
 * - Enables dependency inversion
 * - Allows mocking during testing
 * - Makes transport layer replaceable
 *
 * Example:
 *   Current implementation → FramedConnectionGateway
 *   Future implementation → WebSocketGateway, EncryptedGateway, etc.
 */
public interface ClientMessageGateway {
    void send(Frame frame) throws Exception;

    void sendChat(String message) throws Exception;
}