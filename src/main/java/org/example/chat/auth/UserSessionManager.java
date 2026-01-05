package org.example.chat.auth;

import org.example.chat.ClientHandler;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UserSessionManager {
    private final Map<String, UserSession> sessionsByUsername = new ConcurrentHashMap<>();
    private final Map<Socket, UserSession> sessionsBySocket = new ConcurrentHashMap<>();

    public void registerSession(String username, ClientHandler chatHandler) {
        UserSession session = new UserSession(username, chatHandler);
        sessionsByUsername.put(username, session);
        sessionsBySocket.put(chatHandler.getSocket(), session);
    }

    public Optional<UserSession> getSessionByUsername(String username) {
        return Optional.ofNullable(sessionsByUsername.get(username));
    }

    public Optional<UserSession> getSessionBySocket(Socket socket) {
        return Optional.ofNullable(sessionsBySocket.get(socket));
    }

    public void removeSession(String username) {
        UserSession session = sessionsByUsername.remove(username);
        if (session != null) {
            sessionsBySocket.remove(session.getChatHandler().getSocket());
        }
    }

    public Collection<UserSession> getAllSessions() {
        return Collections.unmodifiableCollection(sessionsByUsername.values());
    }
}
