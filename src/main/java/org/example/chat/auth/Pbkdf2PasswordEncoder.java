package org.example.chat.auth;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Pbkdf2PasswordEncoder implements PasswordEncoder {

    private static final String ID = "pbkdf2";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH = 16;

    private final int iterations;
    private final int keyLength;
    private final SecureRandom secureRandom = new SecureRandom();

    public Pbkdf2PasswordEncoder(int iterations, int keyLength) {
        this.iterations = iterations;
        this.keyLength = keyLength;
    }

    @Override
    public String encode(String rawPassword) {

        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);

        byte[] hash = derive(rawPassword, salt, iterations, keyLength);

        return ID + "$"
                + iterations + "$"
                + keyLength + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    @Override
    public boolean verify(String rawPassword, String encodedPassword) {

        String[] parts = encodedPassword.split("\\$");

        if (parts.length != 5 || !parts[0].equals(ID)) {
            throw new IllegalArgumentException("Invalid encoded password format");
        }

        int storedIterations = Integer.parseInt(parts[1]);
        int storedKeyLength = Integer.parseInt(parts[2]);
        byte[] salt = Base64.getDecoder().decode(parts[3]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[4]);

        byte[] calculatedHash =
                derive(rawPassword, salt, storedIterations, storedKeyLength);

        return MessageDigest.isEqual(calculatedHash, expectedHash);
    }

    @Override
    public boolean needsRehash(String encodedPassword) {

        String[] parts = encodedPassword.split("\\$");
        int storedIterations = Integer.parseInt(parts[1]);
        int storedKeyLength = Integer.parseInt(parts[2]);

        return storedIterations < this.iterations
                || storedKeyLength < this.keyLength;
    }

    private byte[] derive(String password,
                          byte[] salt,
                          int iterations,
                          int keyLength) {

        char[] chars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, keyLength);

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(ALGORITHM);

            return factory.generateSecret(spec).getEncoded();

        } catch (Exception e) {
            throw new IllegalStateException("Hashing failure", e);
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(chars, '\0');
        }
    }
}