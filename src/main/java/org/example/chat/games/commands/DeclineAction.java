package org.example.chat.games.commands;

import org.example.chat.ClientHandler;
import org.example.chat.games.GameService;

public class DeclineAction implements GameAction {

    private final GameService gameService;

    public DeclineAction(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public String name() {
        return "decline";
    }

    @Override
    public void execute(ClientHandler client, String[] args) {

        if (args.length < 2) {
            client.send("Usage: /game decline <player>");
            return;
        }

        gameService.decline(client, args[1]);
    }
}