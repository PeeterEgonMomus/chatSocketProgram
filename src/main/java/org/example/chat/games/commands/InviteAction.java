package org.example.chat.games.commands;

import org.example.chat.ClientHandler;
import org.example.chat.games.*;

/**
 * Design choice:
 * Handles sending a game invite to another player.
 *
 * This class validates input and delegates all business logic
 * to GameService, which coordinates the invite flow.
 *
 * This keeps command handling simple and decoupled from game logic.
 */
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