package org.example.chat.games.commands;

import org.example.chat.ClientHandler;
import org.example.chat.games.GameService;

public class StatsAction implements GameAction {

    private final GameService gameService;

    public StatsAction(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public String name() {
        return "stats";
    }

    @Override
    public void execute(ClientHandler client, String[] args) {

        if (args.length < 2) {
            client.send("Usage: /game stats <game>");
            return;
        }

        gameService.sendStats(client, args[1]);
    }
}