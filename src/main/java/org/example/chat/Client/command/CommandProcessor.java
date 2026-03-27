package org.example.chat.Client.command;

import org.example.chat.Client.connection.FramedChatConnection;

/**
 * Client Command Processing Contract.
 *
 * Responsibility:
 * - Defines the lifecycle of a client-side command processor.
 * - Acts as the abstraction between:
 *      • Input source (console, GUI, script, etc.)
 *      • Messaging layer (connection/gateway)
 *
 * Architecture Role:
 * This is part of the Client Input Layer.
 *
 * It sits above:
 *      - FramedChatConnection (transport)
 *      - Encryption layer
 *
 * And below:
 *      - ClientRuntime (which starts/stops it)
 *
 * Design Purpose:
 * - Allows multiple implementations:
 *      • ConsoleCommandProcessor
 *      • GUICommandProcessor (future)
 *      • Scripted/Bot processor
 *
 * - Keeps input handling interchangeable without affecting:
 *      • Networking
 *      • Protocol handling
 *      • Encryption
 *
 * Lifecycle Model:
 *
 * ClientRuntime
 *      ↓
 * CommandProcessor.start()
 *      ↓
 * Reads user input
 *      ↓
 * Sends messages via gateway/connection
 *
 * Design Pattern:
 * - Strategy Pattern (pluggable input mechanism)
 * - Lifecycle abstraction
 *
 * This interface does NOT:
 * - Define how input is read
 * - Define how commands are parsed
 * - Define how messages are sent
 *
 * It only defines the lifecycle contract.
 */
public interface CommandProcessor {

    /**
     * Starts the command processor.
     *
     * Called by ClientRuntime once the connection
     * and handshake are established.
     *
     * The connection is provided for flexibility,
     * though implementations may delegate through
     * a gateway abstraction instead.
     */
    void start(FramedChatConnection connection);

    /**
     * Stops the command processor.
     *
     * Should:
     * - Stop reading input
     * - Shut down background threads
     * - Release any resources
     */
    void stop();
}