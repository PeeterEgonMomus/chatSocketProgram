package org.example.chat.games;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Design choice:
 * Central registry of all available Game implementations.
 *
 * Acts as a lightweight plugin container for games.
 *
 * This allows:
 * - Registering new games at startup.
 * - Looking up games by name.
 * - Listing all available games dynamically.
 *
 * Uses ConcurrentHashMap to ensure thread-safe access
 * since multiple clients may request games simultaneously.
 *
 * This follows the Registry pattern.
 *
 * GameService depends on this registry rather than
 * hardcoding specific game types (OCP).
 */
public class GameRegistry {

    private final Map<String, Game> games = new ConcurrentHashMap<>();

    public void register(Game game) {
        games.put(game.getName().toLowerCase(), game);
    }

    public Game get(String name) {
        return games.get(name.toLowerCase());
    }

    public boolean exists(String name) {
        return games.containsKey(name.toLowerCase());
    }

    public Collection<Game> getAll() {
        return Collections.unmodifiableCollection(games.values());
    }
}