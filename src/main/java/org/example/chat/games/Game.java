package org.example.chat.games;

import java.util.Set;

/**
 * Design choice:
 * Core abstraction of the game system.
 *
 * This interface defines the contract that every game must follow.
 * The GameManager and GameSession operate only against this abstraction,
 * never against concrete implementations (DIP – Dependency Inversion Principle).
 *
 * This allows:
 * - Adding new games without modifying existing infrastructure (OCP).
 * - Pluggable game logic.
 * - Independent evolution of game rules.
 *
 * Each Game implementation is completely stateless and deterministic
 * with respect to resolving moves. Match state is handled separately
 * inside GameSession.
 *
 * This separation ensures:
 * - Game = rules
 * - GameSession = runtime state
 */
public interface Game {

    String getName();

    Set<String> validMoves();

    GameResult resolveMove(String player1Move, String player2Move);

    String getMoveInstructions();

    default String getHelpText() {
        return "Game: " + getName() + "\n" +
                "Moves: " + String.join(", ", validMoves()) + "\n" +
                "Use: /move <move>";
    }


    int getMoveTimeoutSeconds();
    int getRoundsToWin();
}