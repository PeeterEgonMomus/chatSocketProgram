package org.example.chat.games.commands;

import org.example.chat.ClientHandler;
import org.example.chat.games.GameService;

public class HelpAction implements GameAction {

    private final GameService gameService;

    public HelpAction(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public void execute(ClientHandler client, String[] args) {

        if (args.length < 2) {
            client.send("Usage: /game help <game>");
            return;
        }

        gameService.sendHelp(client, args[1]);
    }
}