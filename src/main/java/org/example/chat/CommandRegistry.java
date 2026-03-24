package org.example.chat;

import java.util.*;

/**
 * Design choice:
 * Centralized command dispatcher.
 *
 * Instead of spreading command lookup and execution logic across the system,
 * this class owns:
 * - command registration
 * - command resolution
 * - cross-cutting concerns (auth enforcement)
 *
 * Key benefits:
 * - Single Responsibility: routing + enforcement in one place
 * - OCP: new commands are added via registration only
 * - Consistency: all commands follow the same execution flow
 *
 * Trade-off:
 * - Becomes a central hub → must avoid adding too many responsibilities
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
     * Design choice:
     * Central execution pipeline.
     *
     * Responsibilities kept here:
     * - parsing
     * - lookup
     * - auth enforcement
     *
     * This prevents duplication across commands.
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