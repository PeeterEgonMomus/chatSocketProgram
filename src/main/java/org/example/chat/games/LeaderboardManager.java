package org.example.chat.games;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LeaderboardManager {

    private final Map<String, Leaderboard> leaderboards = new ConcurrentHashMap<>();

    public Leaderboard getLeaderboard(String gameName) {
        return leaderboards.computeIfAbsent(gameName, k -> new Leaderboard());
    }

    public void recordResult(String gameName,
                             String player1,
                             String player2,
                             GameResult result) {

        Leaderboard lb = getLeaderboard(gameName);

        PlayerStats p1 = lb.getStats(player1);
        PlayerStats p2 = lb.getStats(player2);

        switch (result) {
            case PLAYER1_WIN -> {
                p1.recordWin();
                p2.recordLoss();
            }
            case PLAYER2_WIN -> {
                p2.recordWin();
                p1.recordLoss();
            }
            case DRAW -> {
                p1.recordDraw();
                p2.recordDraw();
            }
        }
    }

    public PlayerStats getPlayerStats(String gameName, String username) {
        return getLeaderboard(gameName).getStats(username);
    }

    public Map<String, PlayerStats> getLeaderboardStats(String gameName) {
        return getLeaderboard(gameName).getAll();
    }
}