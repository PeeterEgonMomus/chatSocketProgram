package org.example.chat.auth;

import org.example.chat.ClientHandler;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for active user sessions.
 *
 * Maintains two lookup maps:
 *
 * 1) username -> session
 * 2) socket   -> session
 *
 * Why two maps?
 * - Fast lookup by username (for messaging)
 * - Fast lookup by socket (for disconnect handling)
 *
 * Thread-safe using ConcurrentHashMap.
 *
 * This acts like a lightweight in-memory session server.
 */
public class UserSessionManager {

    private final Map<String, UserSession> sessionsByUsername =
            new ConcurrentHashMap<>();

    private final Map<Socket, UserSession> sessionsBySocket =
            new ConcurrentHashMap<>();

    /**
     * Registers a new authenticated session.
     */
    public void registerSession(String username, ClientHandler chatHandler) {

        UserSession session = new UserSession(username, chatHandler);

        sessionsByUsername.put(username, session);
        sessionsBySocket.put(chatHandler.getSocket(), session);
    }

    /**
     * Lookup by username.
     */
    public Optional<UserSession> getSessionByUsername(String username) {
        return Optional.ofNullable(sessionsByUsername.get(username));
    }

    /**
     * Lookup by socket (useful for disconnect events).
     */
    public Optional<UserSession> getSessionBySocket(Socket socket) {
        return Optional.ofNullable(sessionsBySocket.get(socket));
    }

    /**
     * Removes session completely.
     * Ensures both maps stay consistent.
     */
    public void removeSession(String username) {

        UserSession session = sessionsByUsername.remove(username);

        if (session != null) {
            sessionsBySocket.remove(
                    session.getChatHandler().getSocket()
            );
        }
    }

    /**
     * Returns immutable view of active sessions.
     */
    public Collection<UserSession> getAllSessions() {
        return Collections.unmodifiableCollection(
                sessionsByUsername.values()
        );
    }
}