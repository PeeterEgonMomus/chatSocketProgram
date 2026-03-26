package org.example.chat.auth;

/**
 * Immutable domain model representing a registered user.
 *
 * Important:
 * - Stores hashed password, NOT plaintext password.
 * - Stores salt used for hashing.
 *
 * Security principle:
 * - Never store plaintext passwords.
 * - Always combine password + unique salt before hashing.
 *
 * Design:
 * - Immutable (final fields)
 * - Thread-safe
 * - No business logic (pure data holder)
 */
public class User {

    private final String username;
    private final String passwordHash;
    private final String salt;

    public User(String username, String passwordHash, String salt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSalt() {
        return salt;
    }
}