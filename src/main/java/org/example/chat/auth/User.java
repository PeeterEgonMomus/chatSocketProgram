package org.example.chat.auth;

/**
 * Immutable domain model representing a registered user.
 *
 * Important:
 * - Stores encoded password (contains algorithm + salt + hash).
 * - Never stores plaintext password.
 *
 * The encodedPassword format:
 * pbkdf2$iterations$keyLength$salt$hash
 */
public class User {

    private final String username;
    private final String encodedPassword;

    public User(String username, String encodedPassword) {
        this.username = username;
        this.encodedPassword = encodedPassword;
    }

    public String getUsername() {
        return username;
    }

    public String getEncodedPassword() {
        return encodedPassword;
    }
}