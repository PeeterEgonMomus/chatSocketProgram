package org.example.chat.games;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

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