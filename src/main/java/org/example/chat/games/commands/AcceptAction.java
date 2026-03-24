package org.example.chat.games.commands;

import org.example.chat.ClientHandler;
import org.example.chat.games.GameService;

public class AcceptAction implements GameAction {

    private final GameService gameService;

    public AcceptAction(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public String name() {
        return "accept";
    }

    @Override
    public void execute(ClientHandler client, String[] args) {

        if (args.length < 2) {
            client.send("Usage: /game accept <player>");
            return;
        }

        gameService.accept(client, args[1]);
    }
}