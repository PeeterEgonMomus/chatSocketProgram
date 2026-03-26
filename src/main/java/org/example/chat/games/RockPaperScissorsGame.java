package org.example.chat.games;

import java.util.Set;

/**
 * Design choice:
 * Strategy-style Game implementation for Rock-Paper-Scissors.
 *
 * This class encapsulates only rule evaluation logic.
 * It contains no networking, session, or leaderboard logic.
 *
 * The switch expression keeps resolution explicit and readable,
 * avoiding deeply nested conditionals.
 *
 * Configured as:
 * - Best of 3 (2 rounds to win)
 * - 10 second move timeout
 *
 * Demonstrates how different games can define:
 * - Custom win conditions
 * - Different match lengths
 * - Different move sets
 *
 * All without modifying GameManager or GameSession (OCP).
 */
public class RockPaperScissorsGame implements Game {

    private static final Set<String> MOVES =
            Set.of("rock", "paper", "scissors");

    @Override
    public String getName() {
        return "rock-paper-scissors";
    }

    @Override
    public Set<String> validMoves() {
        return MOVES;
    }

    @Override
    public String getMoveInstructions() {
        return "Choose your move: rock, paper, or scissors.";
    }

    @Override
    public GameResult resolveMove(String m1, String m2) {

        if (m1.equals(m2)) {
            return GameResult.DRAW;
        }

        return switch (m1) {
            case "rock" ->
                    m2.equals("scissors") ? GameResult.PLAYER1_WIN : GameResult.PLAYER2_WIN;

            case "paper" ->
                    m2.equals("rock") ? GameResult.PLAYER1_WIN : GameResult.PLAYER2_WIN;

            case "scissors" ->
                    m2.equals("paper") ? GameResult.PLAYER1_WIN : GameResult.PLAYER2_WIN;

            default -> throw new IllegalArgumentException("Invalid move: " + m1);
        };
    }

    @Override
    public int getMoveTimeoutSeconds() {
        return 10;
    }

    @Override
    public int getRoundsToWin() {
        return 2;
    }


}