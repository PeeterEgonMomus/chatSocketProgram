package org.example.chat.games.commands;

import org.example.chat.ClientHandler;
import org.example.chat.games.GameService;

/**
 * Design choice:
 * Handles rematch requests between players.
 *
 * This action relies entirely on GameService to determine
 * whether a rematch can be started.
 */
public class RematchAction implements GameAction {

    private final GameService gameService;

    public RematchAction(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public String name() {
        return "rematch";
    }

    @Override
    public void execute(ClientHandler client, String[] args) {
        gameService.rematch(client);
    }
}