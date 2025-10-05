package org.example.chat.auth;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserStore implements UserStore {
    private final Map<String, User> users = new ConcurrentHashMap<>();

    @Override
    public boolean addUser(User user) {
        return users.putIfAbsent(user.getUsername(), user) == null;
    }

    @Override
    public Optional<User> getUser(String username) {
        return Optional.ofNullable(users.get(username));
    }
}
