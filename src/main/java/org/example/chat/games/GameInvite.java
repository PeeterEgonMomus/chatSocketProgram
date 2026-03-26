package org.example.chat.games;

import org.example.chat.ClientHandler;

public class GameInvite {

    private final Game game;
    private final ClientHandler inviter;
    private final ClientHandler invited;

    /**
     * Design choice:
     * Immutable value object representing a pending game invitation.
     *
     * This class contains no behavior — only data.
     * It models the invitation state before a GameSession begins.
     *
     * Keeping it immutable ensures:
     * - Thread-safety
     * - Predictable behavior
     * - No accidental mutation during invite flow
     *
     * GameManager owns lifecycle management of invites.
     */
    private final int moveTimeoutSeconds;

    public GameInvite(Game game,
                      ClientHandler inviter,
                      ClientHandler invited,
                      int moveTimeoutSeconds) {

        this.game = game;
        this.inviter = inviter;
        this.invited = invited;
        this.moveTimeoutSeconds = moveTimeoutSeconds;
    }

    public Game getGame() {
        return game;
    }

    public ClientHandler getInviter() {
        return inviter;
    }

    public ClientHandler getInvited() {
        return invited;
    }

    public int getMoveTimeoutSeconds() {
        return moveTimeoutSeconds;
    }
}