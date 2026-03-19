package org.example.chat.games;

import java.util.Set;

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