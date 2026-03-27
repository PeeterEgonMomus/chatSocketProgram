package org.example.chat.Client.connection;

import org.example.chat.protocol.Frame;

/**
 * Client Transport Abstraction.
 *
 * Responsibility:
 * - Defines how frames are sent and received.
 * - Represents the lowest-level communication contract
 *   used by the client runtime.
 *
 * Architecture Role:
 * This is part of the Client Transport Layer.
 *
 * It sits:
 *      Below  → Runtime, Command Processing, Protocol Dispatching
 *      Above  → Raw Socket / Streams
 *
 * Design Purpose:
 * - Decouples higher layers from concrete transport implementation.
 * - Allows future transport swaps:
 *      • TCP sockets (current)
 *      • WebSocket
 *      • TLS sockets
 *      • Mock connection for testing
 *
 * Design Pattern:
 * - Abstraction Layer
 * - Dependency Inversion (runtime depends on interface, not implementation)
 *
 * This interface does NOT:
 * - Encrypt data
 * - Parse protocol logic
 * - Manage handshake
 *
 * It strictly defines frame-level I/O.
 */
public interface FramedConnection {

    /**
     * Sends a fully constructed protocol frame.
     *
     * Implementation may:
     * - Encrypt payload
     * - Serialize frame
     * - Write to stream
     */
    void send(Frame frame) throws Exception;

    /**
     * Receives the next frame from transport.
     *
     * Returns:
     * - Frame instance
     * - null if connection closed cleanly
     */
    Frame receive() throws Exception;
}