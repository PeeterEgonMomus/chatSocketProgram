package org.example.chat.games.commands;

import org.example.chat.ClientHandler;

/**
 * Design choice:
 * Represents a single game-related user action.
 *
 * This interface follows the Command Pattern, where each implementation
 * encapsulates a specific behavior (e.g., invite, accept, stats).
 *
 * This allows new actions to be added without modifying existing code,
 * making the system Open/Closed Principle (OCP) compliant.
 */
public interface GameAction {

    String name();

    void execute(ClientHandler client, String[] args);
}