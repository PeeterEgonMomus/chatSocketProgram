package org.example.chat.auth;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of UserStore.
 *
 * Stores users in a thread-safe ConcurrentHashMap.
 *
 * Characteristics:
 * - Fast lookup (O(1))
 * - No persistence (data lost on restart)
 * - Safe for multi-threaded access
 *
 * Suitable for:
 * - Development
 * - Testing
 * - Lightweight deployments
 *
 * Design Pattern:
 * - Repository Pattern (concrete implementation)
 */
public class InMemoryUserStore implements UserStore {

    /**
     * Thread-safe map:
     * key   -> username
     * value -> User object
     */
    private final Map<String, User> users = new ConcurrentHashMap<>();

    /**
     * Adds a user only if username does not already exist.
     *
     * putIfAbsent is atomic:
     * - Prevents race conditions during concurrent registration.
     */
    @Override
    public boolean addUser(User user) {
        return users.putIfAbsent(user.getUsername(), user) == null;
    }

    /**
     * Retrieves user safely.
     * Wrapped in Optional to avoid null handling everywhere.
     */
    @Override
    public Optional<User> getUser(String username) {
        return Optional.ofNullable(users.get(username));
    }
}