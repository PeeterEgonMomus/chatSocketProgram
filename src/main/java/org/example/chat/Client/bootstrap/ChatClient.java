package org.example.chat.Client.bootstrap;

import org.example.chat.Client.runtime.ClientRuntime;

/**
 * Client Application Entry Abstraction.
 *
 * Responsibility:
 * - Represents the high-level client application.
 * - Delegates lifecycle control to ClientRuntime.
 *
 * Design Purpose:
 * - Separates application lifecycle from bootstrapping logic.
 * - Keeps "what the client is" separate from "how it is wired".
 *
 * Architecture:
 * ChatClient
 *     → delegates to ClientRuntime
 *         → which manages connection, protocol, and threads
 *
 * This class does NOT:
 * - Manage sockets
 * - Handle encryption
 * - Process commands
 * - Dispatch frames
 *
 * It exists purely as a clean application boundary.
 *
 * Design Pattern:
 * - Facade (very thin)
 * - Lifecycle wrapper
 */
public final class ChatClient {

    private final ClientRuntime runtime;

    /**
     * Dependency injection of the runtime.
     *
     * This keeps ChatClient decoupled from construction details.
     */
    public ChatClient(ClientRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Starts the client runtime.
     *
     * Delegates all initialization and background processes.
     */
    public void start() throws Exception {
        runtime.start();
    }

    /**
     * Stops the client runtime.
     *
     * Ensures graceful shutdown of threads and connections.
     */
    public void stop() {
        runtime.stop();
    }
}