package org.example.chat.games;

import org.example.chat.ClientHandler;

import java.util.Collection;
import java.util.Map;

public class GameService {

    private final GameManager gameManager;
    private final GameRegistry gameRegistry;
    private final LeaderboardManager leaderboardManager;

    public GameService(GameManager gameManager,
                       GameRegistry gameRegistry,
                       LeaderboardManager leaderboardManager) {

        this.gameManager = gameManager;
        this.gameRegistry = gameRegistry;
        this.leaderboardManager = leaderboardManager;
    }

    // =========================
    // 🎮 INVITE FLOW
    // =========================

    public void invite(ClientHandler client, String opponent, String gameName) {

        if (!gameRegistry.exists(gameName)) {
            client.send("Unknown game: " + gameName);
            return;
        }

        var target = client.sessions()
                .getSessionByUsername(opponent)
                .map(s -> s.getChatHandler())
                .orElse(null);

        if (target == null) {
            client.send("Player not found: " + opponent);
            return;
        }

        if (target == client) {
            client.send("You cannot invite yourself.");
            return;
        }

        var game = gameRegistry.get(gameName);

        try {
            gameManager.invite(game, client, target);
        } catch (Exception e) {
            client.send(e.getMessage());
        }

        // ✅ ADD THIS
        client.send("Invite sent to " + opponent + " for " + gameName);
        target.send(client.getUsername() + " invited you to play " + gameName + "type game accept r to start the match");
    }

    // =========================
    // 🎮 ACCEPT FLOW
    // =========================

    public void accept(ClientHandler client, String inviterName) {

        if (inviterName == null || inviterName.isBlank()) {
            client.send("Invalid inviter.");
            return;
        }

        gameManager.acceptInvite(client, inviterName);
    }

    // =========================
    // 🎮 DECLINE FLOW
    // =========================

    public void decline(ClientHandler client, String inviterName) {

        if (inviterName == null || inviterName.isBlank()) {
            client.send("Invalid inviter.");
            return;
        }

        gameManager.declineInvite(client, inviterName);
    }

    // =========================
    // 🎮 REMATCH
    // =========================

    public void rematch(ClientHandler client) {

        // optional extra rule (good practice)
        if (!client.isAuthenticated()) {
            client.send("You must be logged in.");
            return;
        }

        gameManager.requestRematch(client);
    }

    // =========================
    // 🎮 MOVE
    // =========================

    public void submitMove(ClientHandler client, String move) {

        if (move == null || move.isBlank()) {
            client.send("Invalid move.");
            return;
        }

        gameManager.submitMove(client, move);
    }

    // =========================
    // 📊 STATS
    // =========================

    public void sendStats(ClientHandler client, String gameName) {

        if (!gameRegistry.exists(gameName)) {
            client.send("Unknown game: " + gameName);
            return;
        }

        var stats = leaderboardManager.getPlayerStats(
                gameName,
                client.getUsername()
        );

        client.send("Wins: " + stats.getWins());
        client.send("Losses: " + stats.getLosses());
        client.send("Draws: " + stats.getDraws());
    }

    // =========================
    // 🏆 LEADERBOARD
    // =========================

    public void sendLeaderboard(ClientHandler client, String gameName) {

        if (!gameRegistry.exists(gameName)) {
            client.send("Unknown game: " + gameName);
            return;
        }

        Map<String, PlayerStats> statsMap =
                leaderboardManager.getLeaderboardStats(gameName);

        if (statsMap.isEmpty()) {
            client.send("No stats available yet.");
            return;
        }

        statsMap.entrySet().stream()
                .sorted((e1, e2) ->
                        Integer.compare(e2.getValue().getWins(), e1.getValue().getWins()))
                .limit(10)
                .forEach(entry -> {
                    var s = entry.getValue();
                    client.send(entry.getKey()
                            + " | W:" + s.getWins()
                            + " L:" + s.getLosses()
                            + " D:" + s.getDraws());
                });
    }

    // =========================
    // 📋 LIST GAMES
    // =========================

    public void listGames(ClientHandler client) {

        var games = gameRegistry.getAll();

        if (games.isEmpty()) {
            client.send("No games available.");
            return;
        }

        client.send("Available games:");
        games.forEach(g -> client.send(" - " + g.getName()));
    }

    // =========================
    // ❓ HELP
    // =========================

    public void sendHelp(ClientHandler client, String gameName) {

        if (!gameRegistry.exists(gameName)) {
            client.send("Unknown game: " + gameName);
            return;
        }

        client.send(gameRegistry.get(gameName).getHelpText());
    }

    // =========================
    // 🔍 OPTIONAL RAW ACCESS (if needed)
    // =========================

    public Collection<Game> getAllGames() {
        return gameRegistry.getAll();
    }
}