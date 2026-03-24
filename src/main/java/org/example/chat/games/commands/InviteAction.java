package org.example.chat.games.commands;

import org.example.chat.ClientHandler;
import org.example.chat.games.*;

public class InviteAction implements GameAction {

    private final GameService gameService;

    public InviteAction(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public String name() {
        return "invite";
    }

    @Override
    public void execute(ClientHandler client, String[] args) {

        if (args.length < 3) {
            client.send("Usage: /game invite <player> <game>");
            return;
        }

        gameService.invite(client, args[1], args[2]);
    }
}