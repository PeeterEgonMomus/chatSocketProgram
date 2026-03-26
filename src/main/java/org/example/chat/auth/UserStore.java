package org.example.chat.auth;

import java.util.Optional;

/**
 * Abstraction for a user persistence mechanism.
 *
 * This interface defines how users are stored and retrieved,
 * without exposing the underlying storage implementation.
 *
 * Possible implementations:
 * - InMemoryUserStore (current)
 * - DatabaseUserStore (JDBC / JPA)
 * - FileBasedUserStore
 * - RemoteAuthServiceUserStore
 *
 * Design Pattern:
 * - Repository Pattern (simplified)
 * - Strategy Pattern (pluggable storage implementation)
 *
 * This keeps authentication logic decoupled from storage details.
 */
public interface UserStore {

    /**
     * Adds a new user to the store.
     *
     * @param user The user to add
     * @return false if a user with the same username already exists
     */
    boolean addUser(User user);

    /**
     * Retrieves a user by username.
     *
     * @param username The unique username
     * @return Optional containing the user if found
     */
    Optional<User> getUser(String username);
}