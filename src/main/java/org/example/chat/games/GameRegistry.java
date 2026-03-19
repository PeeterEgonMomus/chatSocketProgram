package org.example.chat.games;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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