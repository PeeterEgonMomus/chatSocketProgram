package org.example.chat.games;

import org.example.chat.ClientHandler;

import java.util.concurrent.*;

public class GameSession {

    private final LeaderboardManager leaderboardManager;
    private final GameManager gameManager;

    private final Game game;

    private final ClientHandler player1;
    private final ClientHandler player2;

    private String move1;
    private String move2;

    private final int roundsToWin;

    private int player1Wins = 0;
    private int player2Wins = 0;

    private final int moveTimeoutSeconds;

    // ✅ NEW: timer infrastructure
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> timeoutTask;

    public GameSession(Game game,
                       ClientHandler player1,
                       ClientHandler player2,
                       LeaderboardManager leaderboardManager,
                       GameManager gameManager,
                       int roundsToWin,
                       int moveTimeoutSeconds) {

        this.game = game;
        this.player1 = player1;
        this.player2 = player2;
        this.leaderboardManager = leaderboardManager;
        this.gameManager = gameManager;
        this.roundsToWin = roundsToWin;
        this.moveTimeoutSeconds = moveTimeoutSeconds;

        // ✅ Start first round immediately
        startNextRound();
    }

    public synchronized void submitMove(ClientHandler player, String move) {

        if (!game.validMoves().contains(move)) {
            player.send("Invalid move: " + move);
            return;
        }

        if (player == player1) {
            move1 = move;
            player1.send("Move received. Waiting for opponent...");
        } else if (player == player2) {
            move2 = move;
            player2.send("Move received. Waiting for opponent...");
        }

        checkGameComplete();
    }

    private void checkGameComplete() {

        if (move1 == null || move2 == null) {
            return;
        }

        // ✅ Stop timeout when both moves are in
        cancelTimer();

        GameResult roundResult = game.resolveMove(move1, move2);

        handleRoundResult(roundResult);

        move1 = null;
        move2 = null;
    }

    private void handleRoundResult(GameResult result) {

        switch (result) {

            case PLAYER1_WIN -> {
                player1Wins++;
                player1.send("You win this round!");
                player2.send("You lose this round!");
            }

            case PLAYER2_WIN -> {
                player2Wins++;
                player2.send("You win this round!");
                player1.send("You lose this round!");
            }

            case DRAW -> {
                player1.send("Round is a draw!");
                player2.send("Round is a draw!");
            }
        }

        sendScore();

        checkMatchWinner();
    }

    private void sendScore() {
        String score = "Score: "
                + player1.getUsername() + " [" + player1Wins + "] - "
                + player2.getUsername() + " [" + player2Wins + "]";

        player1.send(score);
        player2.send(score);
    }

    private void checkMatchWinner() {

        if (player1Wins >= roundsToWin) {
            endMatch(GameResult.PLAYER1_WIN);
        } else if (player2Wins >= roundsToWin) {
            endMatch(GameResult.PLAYER2_WIN);
        } else {
            startNextRound();
        }
    }

    private void startNextRound() {
        player1.send("Next round! Submit your move.");
        player2.send("Next round! Submit your move.");

        // ✅ Start timeout timer
        startMoveTimer();
    }

    // ✅ NEW: start timer
    private void startMoveTimer() {

        cancelTimer(); // safety

        timeoutTask = scheduler.schedule(() -> {

            synchronized (this) {

                if (move1 != null && move2 != null) return;

                if (move1 == null && move2 == null) {
                    player1.send("⏱ Both players timed out. Round is a draw.");
                    player2.send("⏱ Both players timed out. Round is a draw.");

                    handleRoundResult(GameResult.DRAW);
                }
                else if (move1 == null) {
                    player1.send("⏱ You ran out of time!");
                    player2.send("🎉 Opponent timed out. You win the round!");

                    handleRoundResult(GameResult.PLAYER2_WIN);
                }
                else if (move2 == null) {
                    player2.send("⏱ You ran out of time!");
                    player1.send("🎉 Opponent timed out. You win the round!");

                    handleRoundResult(GameResult.PLAYER1_WIN);
                }

                // ✅ CRITICAL FIX
                move1 = null;
                move2 = null;
            }

        }, moveTimeoutSeconds, TimeUnit.SECONDS);
    }

    // ✅ NEW: cancel timer
    private void cancelTimer() {
        if (timeoutTask != null && !timeoutTask.isDone()) {
            timeoutTask.cancel(true);
        }
    }

    private void endMatch(GameResult result) {

        switch (result) {

            case PLAYER1_WIN -> {
                player1.send("🏆 You won the match!");
                player2.send("💀 You lost the match!");
            }

            case PLAYER2_WIN -> {
                player2.send("🏆 You won the match!");
                player1.send("💀 You lost the match!");
            }
        }

        leaderboardManager.recordResult(
                game.getName(),
                player1.getUsername(),
                player2.getUsername(),
                result
        );

        player1.send("Game over.");
        player2.send("Game over.");

        player1.send("Type /game rematch to play again.");
        player2.send("Type /game rematch to play again.");

        // ✅ CLEANUP
        cancelTimer();
        scheduler.shutdownNow();

        gameManager.endGame(this);
    }

    public ClientHandler getOpponent(ClientHandler player) {

        if (player == player1) return player2;
        if (player == player2) return player1;

        return null;
    }

    public ClientHandler getPlayer1() {
        return player1;
    }

    public ClientHandler getPlayer2() {
        return player2;
    }
}