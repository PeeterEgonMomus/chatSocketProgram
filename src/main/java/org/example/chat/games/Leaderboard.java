package org.example.chat.games;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Leaderboard {

    private final Map<String, PlayerStats> stats = new ConcurrentHashMap<>();

    public PlayerStats getStats(String username) {
        return stats.computeIfAbsent(username, k -> new PlayerStats());
    }

    public Map<String, PlayerStats> getAll() {
        return stats;
    }
}