package org.example.chat;

import java.util.*;

/**
 * Design choice:
 * Centralized Command Dispatch Engine.
 *
 * This class implements the Command Pattern coordinator.
 *
 * Responsibilities:
 * - Command registration
 * - Command lookup
 * - Authentication enforcement
 * - Execution pipeline
 *
 * ---------------------------------------------------------
 * Architectural Role
 * ---------------------------------------------------------
 *
 * This sits above the Protocol Layer and below ClientHandler.
 *
 * Flow:
 *
 * Client sends text
 *     ↓
 * ClientHandler extracts string
 *     ↓
 * CommandRegistry.executeCommand(...)
 *     ↓
 * Command.execute(...)
 *
 * ---------------------------------------------------------
 * Why Centralize Execution?
 * ---------------------------------------------------------
 *
 * Without this registry:
 * - ClientHandler would grow large
 * - Auth checks would be duplicated
 * - Parsing logic would scatter
 *
 * Centralizing provides:
 * - Consistency
 * - Single enforcement point
 * - Easy extensibility
 *
 * ---------------------------------------------------------
 * Design Principles Applied:
 * ---------------------------------------------------------
 *
 * - Single Responsibility
 * - Open/Closed Principle
 * - Strategy Pattern
 * - Plugin architecture (via ServiceLoader)
 */
public class CommandRegistry {

    private final Map<String, Command> commands = new HashMap<>();

    public CommandRegistry() {
        /**
         * Design choice:
         * ServiceLoader enables plugin-style command discovery.
         *
         * Commands can be added without modifying this class,
         * supporting modular architectures.
         *
         * Trade-off:
         * - More implicit behavior (can be harder to debug)
         */
        ServiceLoader<Command> loader = ServiceLoader.load(Command.class);
        for (Command cmd : loader) {
            register(cmd);
        }
    }

    /**
     * Design choice:
     * Explicit registration keeps wiring flexible (manual or automatic).
     */
    public void register(Command command) {
        commands.put(command.getName().toUpperCase(), command);
    }

    /**
     * Main execution pipeline for commands.
     *
     * Steps:
     * 1. Validate input
     * 2. Parse command name
     * 3. Lookup command
     * 4. Enforce authentication
     * 5. Extract arguments
     * 6. Delegate execution
     *
     * This method intentionally contains
     * the cross-cutting concerns of command execution.
     */
    public void executeCommand(ClientHandler client, String input) {

        if (input == null || input.isBlank()) return;

        String[] parts = input.trim().split("\\s+");
        String cmdName = parts[0].toUpperCase();

        Command command = commands.get(cmdName);

        if (command == null) {
            client.send("Unknown command: " + cmdName);
            return;
        }

        /**
         * Design choice:
         * Centralized authorization logic.
         *
         * Commands declare intent (requiresAuth),
         * but enforcement happens here.
         *
         * This avoids duplicating auth checks inside every command.
         */
        if (command.requiresAuth() && !client.isAuthenticated()) {
            client.send("You must log in first. Use: LOGIN <username> <password>");
            return;
        }

        String[] args = parts.length > 1
                ? Arrays.copyOfRange(parts, 1, parts.length)
                : new String[0];

        command.execute(client, args);
    }

    /**
     * Design choice:
     * Read-only exposure prevents accidental mutation
     * and protects internal state.
     */
    public Set<String> getCommandNames() {
        return Collections.unmodifiableSet(commands.keySet());
    }
}