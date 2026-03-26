package org.example.chat.games;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Design choice:
 * Represents leaderboard data for a single game.
 *
 * It maps usernames to PlayerStats.
 *
 * Uses ConcurrentHashMap to allow concurrent updates
 * from multiple game sessions.
 *
 * This class is intentionally simple and focused
 * purely on data storage.
 *
 * LeaderboardManager handles higher-level orchestration.
 */
public class Leaderboard {

    private final Map<String, PlayerStats> stats = new ConcurrentHashMap<>();

    public PlayerStats getStats(String username) {
        return stats.computeIfAbsent(username, k -> new PlayerStats());
    }

    public Map<String, PlayerStats> getAll() {
        return stats;
    }
}