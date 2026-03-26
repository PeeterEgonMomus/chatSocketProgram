package org.example.chat.games;

/**
 * Design choice:
 * Represents the result of a single round or match.
 *
 * Using an enum ensures:
 * - Type safety (no string comparison errors)
 * - Exhaustive switch handling
 * - Clear domain modeling
 *
 * The result is always expressed relative to Player1 and Player2.
 * GameSession translates this into user-facing messages.
 *
 * This keeps domain logic separate from presentation logic.
 */
public enum GameResult {
    PLAYER1_WIN,
    PLAYER2_WIN,
    DRAW
}