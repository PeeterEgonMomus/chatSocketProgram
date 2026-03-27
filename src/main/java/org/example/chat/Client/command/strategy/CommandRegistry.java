package org.example.chat.Client.command.strategy;

import java.util.List;


/**
 * Central dispatcher for command strategies.
 *
 * Maintains a list of available CommandStrategy implementations
 * and delegates execution to the first strategy that supports
 * the given user input.
 *
 * Design:
 * - Implements simple chain-of-responsibility style dispatch
 * - Decouples command processing from command definitions
 *
 * Behavior:
 * - Iterates strategies in registration order
 * - Executes first matching strategy
 * - Returns false if no command matches (treated as normal chat)
 */
public final class CommandRegistry {

    private final List<CommandStrategy> strategies;

    public CommandRegistry(List<CommandStrategy> strategies) {
        this.strategies = strategies;
    }

    public boolean dispatch(String input) throws Exception {

        for (CommandStrategy s : strategies) {
            if (s.supports(input)) {
                s.execute(input);
                return true;
            }
        }

        return false;
    }
}