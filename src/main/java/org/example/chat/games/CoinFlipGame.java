package org.example.chat.games;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Design choice:
 * Concrete Game implementation representing a simple coin flip match.
 *
 * This class contains only rule logic and no session or player state.
 * It is therefore fully stateless and thread-safe.
 *
 * Randomness is encapsulated inside resolveMove(),
 * while match lifecycle and timing are handled by GameSession.
 *
 * Keeping randomness inside the Game implementation ensures:
 * - Each game defines its own resolution mechanics.
 * - GameSession remains generic and reusable.
 *
 * The game is configured as:
 * - Single round match
 * - 10 second timeout
 *
 * This demonstrates how different games can customize match rules
 * without changing infrastructure code.
 */
public class CoinFlipGame implements Game {

    private static final Set<String> MOVES =
            Set.of("heads", "tails");

    @Override
    public String getName() {
        return "coin-flip";
    }

    @Override
    public Set<String> validMoves() {
        return MOVES;
    }

    @Override
    public String getMoveInstructions() {
        return "Choose: heads or tails.";
    }

    @Override
    public GameResult resolveMove(String m1, String m2) {

        String result = ThreadLocalRandom.current().nextBoolean()
                ? "heads"
                : "tails";

        boolean p1Correct = m1.equals(result);
        boolean p2Correct = m2.equals(result);

        if (p1Correct && p2Correct) {
            return GameResult.DRAW;
        }

        if (p1Correct) {
            return GameResult.PLAYER1_WIN;
        }

        if (p2Correct) {
            return GameResult.PLAYER2_WIN;
        }

        return GameResult.DRAW;
    }

    @Override
    public int getMoveTimeoutSeconds() {
        return 10;
    }

    @Override
    public int getRoundsToWin() {
        return 1; // usually coin flip = single round
    }
}