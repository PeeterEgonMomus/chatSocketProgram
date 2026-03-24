package org.example.chat;

/**
 * Design choice:
 * This defines the Command Pattern contract.
 *
 * Instead of hardcoding logic in if/else or switch statements,
 * each action is encapsulated as its own object.
 *
 * Key benefits:
 * - OCP: new commands can be added without modifying existing code
 * - Decoupling: ClientHandler does not need to know command logic
 * - Testability: commands can be tested in isolation
 *
 * Trade-off:
 * - Slight increase in number of classes (intentional for scalability)
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