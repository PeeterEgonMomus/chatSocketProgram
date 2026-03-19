package org.example.chat.games;

import org.example.chat.ClientHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {

    private final Map<String, GameInvite> invites = new ConcurrentHashMap<>();
    private final Map<ClientHandler, GameSession> activeGames = new ConcurrentHashMap<>();
    private final Map<ClientHandler, ClientHandler> rematchRequests = new ConcurrentHashMap<>();
    private final Map<ClientHandler, ClientHandler> lastOpponent = new ConcurrentHashMap<>();
    private final Map<ClientHandler, Game> lastGame = new ConcurrentHashMap<>();

    private final LeaderboardManager leaderboardManager;

    public GameManager(LeaderboardManager leaderboardManager) {
        this.leaderboardManager = leaderboardManager;
    }

    public void invite(Game game,
                       ClientHandler inviter,
                       ClientHandler invited) {

        if (activeGames.containsKey(inviter)) {
            inviter.send("You are already in a game.");
            return;
        }

        if (activeGames.containsKey(invited)) {
            inviter.send(invited + " is already in a game.");
            return;
        }

        int timeout = game.getMoveTimeoutSeconds();

        GameInvite invite = new GameInvite(game, inviter, invited, timeout);

        invites.put(invited.getUsername(), invite);

        invited.send(inviter.getUsername() + " invited you to play "
                + game.getName() + " (timeout: " + timeout + "s)");
        invited.send("Type /game accept " + inviter.getUsername());
    }

    public void acceptInvite(ClientHandler invited, String inviterName) {

        GameInvite invite = invites.remove(invited.getUsername());

        if (invite == null) {
            invited.send("No pending game invite.");
            return;
        }

        ClientHandler inviter = invite.getInviter();

        if (!inviter.getUsername().equals(inviterName)) {
            invited.send("Invite mismatch.");
            return;
        }

        Game game = invite.getGame();

        GameSession session = new GameSession(
                game,
                inviter,
                invited,
                leaderboardManager,
                this,
                game.getRoundsToWin(),
                game.getMoveTimeoutSeconds()
        );

        activeGames.put(inviter, session);
        activeGames.put(invited, session);

        lastOpponent.put(inviter, invited);
        lastOpponent.put(invited, inviter);

        inviter.send(invited.getUsername() + " accepted your game invite!");
        invited.send("You accepted the game invite.");

        inviter.send("Game started: " + game.getName());
        invited.send("Game started: " + game.getName());

        String instructions = game.getMoveInstructions();

        inviter.send(instructions);
        invited.send(instructions);

        inviter.send("Submit your move using: /move <move>");
        invited.send("Submit your move using: /move <move>");

        lastGame.put(inviter, game);
        lastGame.put(invited, game);
    }

    public void requestRematch(ClientHandler player) {

        GameSession session = activeGames.get(player);

        if (session != null) {
            player.send("You are still in a game.");
            return;
        }

        ClientHandler opponent = findLastOpponent(player);

        if (opponent == null) {
            player.send("No recent opponent found.");
            return;
        }

        rematchRequests.put(player, opponent);

        player.send("Rematch requested. Waiting for opponent...");

        if (rematchRequests.get(opponent) == player) {
            startRematch(player, opponent);
        } else {
            opponent.send(player.getUsername() + " wants a rematch. Type /game rematch to accept.");
        }
    }

    private void startRematch(ClientHandler p1, ClientHandler p2) {

        rematchRequests.remove(p1);
        rematchRequests.remove(p2);

        Game game = getLastGame(p1);

        if (game == null) {
            p1.send("Error starting rematch.");
            p2.send("Error starting rematch.");
            return;
        }

        GameSession newSession = new GameSession(
                game,
                p1,
                p2,
                leaderboardManager,
                this,
                game.getRoundsToWin(),
                game.getMoveTimeoutSeconds()
        );

        activeGames.put(p1, newSession);
        activeGames.put(p2, newSession);

        p1.send("Rematch started!");
        p2.send("Rematch started!");

        String instructions = game.getMoveInstructions();

        p1.send(instructions);
        p2.send(instructions);
    }

    public void declineInvite(ClientHandler invited, String inviterName) {

        GameInvite invite = invites.remove(invited.getUsername());

        if (invite == null) {
            invited.send("No pending game invite.");
            return;
        }

        ClientHandler inviter = invite.getInviter();

        if (!inviter.getUsername().equals(inviterName)) {
            invited.send("Invite mismatch.");
            return;
        }

        inviter.send(invited + " declined your game invite.");
        invited.send("Game invite declined.");
    }

    public void submitMove(ClientHandler player, String move) {

        GameSession session = activeGames.get(player);

        if (session == null) {
            player.send("You are not in a game.");
            return;
        }

        session.submitMove(player, move);
    }

    public void endGame(GameSession session) {
        activeGames.remove(session.getPlayer1());
        activeGames.remove(session.getPlayer2());
    }

    private ClientHandler findLastOpponent(ClientHandler player) {
        return lastOpponent.get(player);
    }

    private Game getLastGame(ClientHandler player) {
        return lastGame.get(player);
    }
}