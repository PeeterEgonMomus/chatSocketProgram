package org.example.chat;

/**
 * Design choice:
 * Command Pattern abstraction.
 *
 * Each command encapsulates:
 * - A name
 * - Its execution logic
 * - Optional metadata (auth requirement, description)
 *
 * ---------------------------------------------------------
 * Architectural Role
 * ---------------------------------------------------------
 *
 * Commands represent user-triggered actions.
 *
 * Instead of:
 *   if (cmd.equals("LOGIN")) { ... }
 *
 * We use:
 *   Command objects with polymorphism.
 *
 * ---------------------------------------------------------
 * Benefits:
 * ---------------------------------------------------------
 *
 * - Extensible without modifying dispatcher
 * - Isolated logic
 * - Easier unit testing
 * - Supports plugin discovery
 *
 * ---------------------------------------------------------
 * Default Methods:
 * ---------------------------------------------------------
 *
 * requiresAuth():
 *   Allows declarative security.
 *
 * getDescription():
 *   Enables future help system expansion.
 *
 * This design anticipates future growth.
 */
public interface Command {

    String getName();

    void execute(ClientHandler client, String[] args);

    /**
     * Design choice:
     * Default method avoids forcing every command
     * to care about authentication.
     *
     * This keeps commands focused on their domain logic
     * while allowing centralized enforcement elsewhere.
     */
    default boolean requiresAuth() { return false; }

    /**
     * Design choice:
     * Enables future extensibility (e.g., richer help system)
     * without modifying the interface later.
     */
    default String getDescription() { return ""; }
}