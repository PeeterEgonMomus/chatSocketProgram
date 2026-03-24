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

    // =========================
    // 🎮 INVITE
    // =========================

    public void invite(Game game,
                       ClientHandler inviter,
                       ClientHandler invited) {

        if (activeGames.containsKey(inviter)) {
            throw new IllegalStateException("You are already in a game.");
        }

        if (activeGames.containsKey(invited)) {
            throw new IllegalStateException(invited + " is already in a game.");
        }

        int timeout = game.getMoveTimeoutSeconds();

        GameInvite invite = new GameInvite(game, inviter, invited, timeout);

        invites.put(invited.getUsername(), invite);
    }

    // =========================
    // 🎮 ACCEPT
    // =========================

    public GameSession acceptInvite(ClientHandler invited, String inviterName) {

        GameInvite invite = invites.remove(invited.getUsername());

        if (invite == null) {
            throw new IllegalStateException("No pending game invite.");
        }

        ClientHandler inviter = invite.getInviter();

        if (!inviter.getUsername().equals(inviterName)) {
            throw new IllegalStateException("Invite mismatch.");
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

        lastGame.put(inviter, game);
        lastGame.put(invited, game);

        return session;
    }

    // =========================
    // 🎮 DECLINE
    // =========================

    public ClientHandler declineInvite(ClientHandler invited, String inviterName) {

        GameInvite invite = invites.remove(invited.getUsername());

        if (invite == null) {
            throw new IllegalStateException("No pending game invite.");
        }

        ClientHandler inviter = invite.getInviter();

        if (!inviter.getUsername().equals(inviterName)) {
            throw new IllegalStateException("Invite mismatch.");
        }

        return inviter;
    }

    // =========================
    // 🎮 REMATCH
    // =========================

    public GameSession requestRematch(ClientHandler player) {

        GameSession session = activeGames.get(player);

        if (session != null) {
            throw new IllegalStateException("You are still in a game.");
        }

        ClientHandler opponent = lastOpponent.get(player);

        if (opponent == null) {
            throw new IllegalStateException("No recent opponent found.");
        }

        rematchRequests.put(player, opponent);

        if (rematchRequests.get(opponent) == player) {
            return startRematch(player, opponent);
        }

        return null; // waiting for opponent
    }

    private GameSession startRematch(ClientHandler p1, ClientHandler p2) {

        rematchRequests.remove(p1);
        rematchRequests.remove(p2);

        Game game = lastGame.get(p1);

        if (game == null) {
            throw new IllegalStateException("Error starting rematch.");
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

        return newSession;
    }

    // =========================
    // 🎮 MOVE
    // =========================

    public GameSession submitMove(ClientHandler player, String move) {

        GameSession session = activeGames.get(player);

        if (session == null) {
            throw new IllegalStateException("You are not in a game.");
        }

        session.submitMove(player, move);

        return session;
    }

    // =========================
    // 🏁 END GAME
    // =========================

    public void endGame(GameSession session) {
        activeGames.remove(session.getPlayer1());
        activeGames.remove(session.getPlayer2());
    }
}