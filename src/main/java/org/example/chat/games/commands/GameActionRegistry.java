package org.example.chat.games.commands;

import java.util.HashMap;
import java.util.Map;

/**
 * Design choice:
 * Acts as a registry for all GameAction implementations.
 *
 * This replaces traditional switch/if-else logic with a lookup-based dispatch mechanism.
 *
 * Benefits:
 * - OCP compliant (new actions can be added without modifying existing code)
 * - Centralized action management
 * - Simplifies GameCommand by delegating resolution logic
 */
public class GameActionRegistry {

    private final Map<String, GameAction> actions = new HashMap<>();

    public void register(GameAction action) {
        actions.put(action.name().toLowerCase(), action);
    }

    public GameAction get(String name) {
        return actions.get(name.toLowerCase());
    }
}