package org.example.chat.games.commands;

import org.example.chat.ClientHandler;
import org.example.chat.games.GameService;

/**
 * Design choice:
 * Lists all available games in the system.
 *
 * This action does not require arguments and simply delegates
 * to GameService for retrieval.
 */
public class ListAction implements GameAction {

    private final GameService gameService;

    public ListAction(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public void execute(ClientHandler client, String[] args) {
        gameService.listGames(client);
    }
}