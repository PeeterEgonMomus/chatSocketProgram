package org.example.chat.games;

/**
 * Design choice:
 * Simple mutable data object representing a player's statistics.
 *
 * Intentionally minimal:
 * - No synchronization
 * - No persistence logic
 *
 * Concurrency safety is handled at the Leaderboard level
 * via ConcurrentHashMap.
 *
 * This keeps PlayerStats lightweight and focused
 * purely on statistical counters.
 */
public class PlayerStats {

    private int wins;
    private int losses;
    private int draws;

    public void recordWin() { wins++; }
    public void recordLoss() { losses++; }
    public void recordDraw() { draws++; }

    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getDraws() { return draws; }
}