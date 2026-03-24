package org.example.chat.games.commands;

import org.example.chat.ClientHandler;
import org.example.chat.games.GameService;

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