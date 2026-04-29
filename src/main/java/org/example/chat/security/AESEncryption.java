package org.example.chat.security;

import org.example.chat.util.Logger;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


/**
 * Design choice:
 * Implements AES-256 encryption using GCM mode.
 *
 * Responsibilities:
 * - Encrypt plaintext bytes using AES
 * - Decrypt ciphertext bytes using AES
 * - Generate secure random AES keys
 * - Attach IV to ciphertext
 * - Support optional AAD (Additional Authenticated Data)
 *
 * Security Model:
 * - AES/GCM provides:
 *     • Confidentiality
 *     • Integrity (authentication tag)
 * - Each encryption generates a new random IV
 * - IV is prepended to ciphertext for transport
 *
 * It does NOT:
 * - Manage client sessions
 * - Perform handshake
 * - Store client mappings
 *
 * Architectural Role:
 * - Session-level symmetric encryption
 * - Used after handshake is completed
 *
 * Safety Features:
 * - 256-bit key strength
 * - 128-bit authentication tag
 * - Random IV per message
 *
 * Logging:
 * - Debug logs for encryption/decryption size tracing
 * - Error logs for cryptographic failures
 */
/**
 * AES-256 GCM encryption (session-level symmetric crypto)
 */
public class AESEncryption implements SymmetricEncryption {

    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int AES_KEY_BITS = 256;
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey key;

    public AESEncryption(byte[] keyBytes) {
        if (keyBytes == null || keyBytes.length == 0) {
            throw new IllegalArgumentException("AES key cannot be null or empty");
        }

        this.key = new SecretKeySpec(keyBytes.clone(), "AES");

        Logger.info("AESEncryption initialized (key length=" + keyBytes.length + " bytes)");
    }

    @Override
    public byte[] encrypt(byte[] plainText, byte[] aad) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));

            if (aad != null) {
                cipher.updateAAD(aad);
            }

            byte[] cipherText = cipher.doFinal(plainText);

            byte[] result = new byte[IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, result, 0, IV_LENGTH);
            System.arraycopy(cipherText, 0, result, IV_LENGTH, cipherText.length);

            Logger.debug("AES encrypt | plaintext=" + plainText.length +
                    ", ciphertext=" + result.length);

            return result;

        } catch (Exception e) {
            Logger.error("AES encryption failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] decrypt(byte[] cipherBytes, byte[] aad) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(cipherBytes, 0, iv, 0, IV_LENGTH);

            byte[] cipherText = new byte[cipherBytes.length - IV_LENGTH];
            System.arraycopy(cipherBytes, IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));

            if (aad != null) {
                cipher.updateAAD(aad);
            }

            byte[] decrypted = cipher.doFinal(cipherText);

            Logger.debug("AES decrypt | ciphertext=" + cipherBytes.length +
                    ", plaintext=" + decrypted.length);

            return decrypted;

        } catch (Exception e) {
            Logger.error("AES decryption failed", e);
            throw new RuntimeException(e);
        }
    }
}