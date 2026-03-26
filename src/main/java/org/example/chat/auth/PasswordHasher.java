package org.example.chat.auth;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Utility class for securely hashing passwords.
 *
 * Uses:
 * - PBKDF2WithHmacSHA256
 * - Per-user random salt
 * - 65,536 iterations
 * - 256-bit derived key
 *
 * Why PBKDF2?
 * - Slows down brute-force attacks
 * - Industry-standard key derivation function
 *
 * Security Architecture:
 * password + unique salt -> PBKDF2 -> stored hash
 *
 * Design:
 * - Stateless
 * - Static utility class
 * - Non-instantiable (private constructor)
 */
public final class PasswordHasher {

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    private PasswordHasher() {}

    /**
     * Generates a cryptographically secure random salt.
     *
     * Each user must have a unique salt.
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Derives a hash from password + salt using PBKDF2.
     *
     * @param password Raw password
     * @param saltBase64 Base64-encoded salt
     * @return Base64-encoded hash
     */
    public static String hash(String password, String saltBase64) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);

            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    ITERATIONS,
                    KEY_LENGTH
            );

            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            byte[] hash = factory.generateSecret(spec).getEncoded();

            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifies a password attempt.
     *
     * Recomputes the hash using stored salt
     * and compares with stored hash.
     */
    public static boolean verify(String password,
                                 String saltBase64,
                                 String expectedHash) {

        return hash(password, saltBase64).equals(expectedHash);
    }
}