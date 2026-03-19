package org.example.chat.commands;

import org.example.chat.ClientHandler;
import org.example.chat.Command;
import org.example.chat.games.*;

public class GameCommand implements Command {

    private final GameManager gameManager;
    private final GameRegistry gameRegistry;
    private final LeaderboardManager leaderboardManager;


    public GameCommand(GameManager gameManager,
                       GameRegistry gameRegistry,
                       LeaderboardManager leaderboardManager) {

        this.gameManager = gameManager;
        this.gameRegistry = gameRegistry;
        this.leaderboardManager = leaderboardManager;
    }

    @Override
    public String getName() {
        return "game";
    }

    @Override
    public void execute(ClientHandler client, String[] args) {

        if (args.length < 1) {
            client.send("Usage: /game invite <player> <game>");
            return;
        }

        String action = args[0];

        switch (action) {

            case "list" -> listGames(client);

            case "invite" -> invite(client, args);

            case "accept" -> accept(client, args);

            case "decline" -> decline(client, args);

            case "help" -> help(client, args);

            case "rematch" -> rematch(client);

            case "stats" -> stats(client, args);
            case "leaderboard" -> leaderboard(client, args);

            default -> client.send("Unknown game command.");
        }
    }

    private void invite(ClientHandler client, String[] args) {

        if (args.length < 3) {
            client.send("Usage: /game invite <player> <game>");
            return;
        }

        String opponent = args[1];
        String gameName = args[2];

        if (!gameRegistry.exists(gameName)) {
            client.send("Unknown game: " + gameName);
            return;
        }

        var target =
                client.sessions()
                        .getSessionByUsername(opponent)
                        .map(s -> s.getChatHandler())
                        .orElse(null);

        if (target == null) {
            client.send("Player not found: " + opponent);
            return;
        }

        Game game = gameRegistry.get(gameName);

        gameManager.invite(game, client, target);
    }

    private void accept(ClientHandler client, String[] args) {

        if (args.length < 2) {
            client.send("Usage: /game accept <player>");
            return;
        }

        gameManager.acceptInvite(client, args[1]);
    }

    private void decline(ClientHandler client, String[] args) {

        if (args.length < 2) {
            client.send("Usage: /game decline <player>");
            return;
        }

        gameManager.declineInvite(client, args[1]);
    }

    private void listGames(ClientHandler client) {

        var games = gameRegistry.getAll();

        if (games.isEmpty()) {
            client.send("No games available.");
            return;
        }

        client.send("Available games:");

        for (Game game : games) {
            client.send(" - " + game.getName());
        }
    }

    private void help(ClientHandler client, String[] args) {

        if (args.length < 2) {
            client.send("Usage: /game help <game>");
            return;
        }

        String gameName = args[1];

        if (!gameRegistry.exists(gameName)) {
            client.send("Unknown game: " + gameName);
            return;
        }

        Game game = gameRegistry.get(gameName);

        client.send(game.getHelpText());
    }

    private void stats(ClientHandler client, String[] args) {

        if (args.length < 2) {
            client.send("Usage: /game stats <game>");
            return;
        }

        String gameName = args[1];

        if (!gameRegistry.exists(gameName)) {
            client.send("Unknown game: " + gameName);
            return;
        }

        PlayerStats stats = leaderboardManager.getPlayerStats(
                gameName,
                client.getUsername()
        );

        client.send("=== Your stats for " + gameName + " ===");
        client.send("Wins: " + stats.getWins());
        client.send("Losses: " + stats.getLosses());
        client.send("Draws: " + stats.getDraws());
    }

    private void leaderboard(ClientHandler client, String[] args) {

        if (args.length < 2) {
            client.send("Usage: /game leaderboard <game>");
            return;
        }

        String gameName = args[1];

        if (!gameRegistry.exists(gameName)) {
            client.send("Unknown game: " + gameName);
            return;
        }

        var statsMap = leaderboardManager.getLeaderboardStats(gameName);

        if (statsMap.isEmpty()) {
            client.send("No stats available yet.");
            return;
        }

        client.send("=== Leaderboard: " + gameName + " ===");

        statsMap.entrySet().stream()
                .sorted((e1, e2) ->
                        Integer.compare(
                                e2.getValue().getWins(),
                                e1.getValue().getWins()
                        )
                )
                .limit(10)
                .forEach(entry -> {
                    String user = entry.getKey();
                    PlayerStats s = entry.getValue();

                    client.send(user + " | W:" + s.getWins()
                            + " L:" + s.getLosses()
                            + " D:" + s.getDraws());
                });
    }

    private void rematch(ClientHandler client) {
        gameManager.requestRematch(client);
    }
}