package org.example.chat.games.commands;

import java.util.HashMap;
import java.util.Map;

public class GameActionRegistry {

    private final Map<String, GameAction> actions = new HashMap<>();

    public void register(GameAction action) {
        actions.put(action.name().toLowerCase(), action);
    }

    public GameAction get(String name) {
        return actions.get(name.toLowerCase());
    }
}