package org.example.chat.games.commands;

import org.example.chat.ClientHandler;
import org.example.chat.games.GameService;

/**
 * Design choice:
 * Retrieves leaderboard data for a specific game.
 *
 * This action is read-only and delegates to GameService,
 * ensuring that all data access is centralized.
 */
public class LeaderboardAction implements GameAction {

    private final GameService gameService;

    public LeaderboardAction(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public String name() {
        return "leaderboard";
    }

    @Override
    public void execute(ClientHandler client, String[] args) {

        if (args.length < 2) {
            client.send("Usage: /game leaderboard <game>");
            return;
        }

        gameService.sendLeaderboard(client, args[1]);
    }
}