package org.example.chat.commands;

import org.example.chat.ClientHandler;
import org.example.chat.Command;
import org.example.chat.games.GameService;

/**
 * Design choice:
 * Commands delegate to GameService (application layer),
 * NOT directly to GameManager.
 *
 * This ensures a single entry point for all game logic
 * (commands + network frames).
 */
public class MoveCommand implements Command {

    private final GameService gameService;

    public MoveCommand(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public String getName() {
        return "move";
    }

    @Override
    public void execute(ClientHandler client, String[] args) {

        if (args.length < 1) {
            client.send("Usage: /move <move>");
            return;
        }

        String move = args[0];

        gameService.submitMove(client, move);
    }
}