package org.example.chat.security;

import org.example.chat.util.Logger;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class AESEncryption implements EncryptionStrategy {
    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int AES_KEY_BITS = 256;
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String base64Key;

    public AESEncryption(String base64Key) {
        this.base64Key = base64Key;
        Logger.info("AESEncryption initialized with key length=" + base64Key.length());
    }

    public static String generateKeyBase64() {
        try {
            Logger.debug("Generating new AES-256 key...");
            KeyGenerator gen = KeyGenerator.getInstance("AES");
            gen.init(AES_KEY_BITS, RANDOM);
            SecretKey key = gen.generateKey();
            String base64 = Base64.getEncoder().encodeToString(key.getEncoded());
            Logger.debug("Generated AES key (Base64 length=" + base64.length() + ")");
            return base64;
        } catch (Exception e) {
            Logger.error("Failed to generate AES key", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String encrypt(String plainText) {
        try {
            byte[] encrypted = encryptBytes(plainText.getBytes(), null);
            String result = Base64.getEncoder().encodeToString(encrypted);
            Logger.debug("AES encryption done (input=" + plainText.length() + "B, output=" + result.length() + ")");
            return result;
        } catch (Exception e) {
            Logger.error("AES encryption failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = decryptBytes(cipherBytes, null);
            String text = new String(decrypted);
            Logger.debug("AES decryption done (plain length=" + text.length() + ")");
            return text;
        } catch (Exception e) {
            Logger.error("AES decryption failed", e);
            throw new RuntimeException(e);
        }
    }

    /** Encrypt raw bytes for file-safe transmission */
    public byte[] encryptBytes(byte[] plainBytes, byte[] aad) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));

            if (aad != null) cipher.updateAAD(aad);

            byte[] ct = cipher.doFinal(plainBytes);

            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);

            Logger.debug("AES encryptBytes | plaintext=" + plainBytes.length + ", ciphertext=" + out.length);
            return out;
        } catch (Exception e) {
            Logger.error("AES byte encryption failed", e);
            throw new RuntimeException(e);
        }
    }

    public byte[] decryptBytes(byte[] cipherBytes, byte[] aad) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(cipherBytes, 0, iv, 0, IV_LENGTH);

            byte[] ct = new byte[cipherBytes.length - IV_LENGTH];
            System.arraycopy(cipherBytes, IV_LENGTH, ct, 0, ct.length);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));

            if (aad != null) cipher.updateAAD(aad);

            byte[] decrypted = cipher.doFinal(ct);

            Logger.debug("AES decryptBytes | ciphertext=" + cipherBytes.length + ", plaintext=" + decrypted.length);
            return decrypted;
        } catch (Exception e) {
            Logger.error("AES byte decryption failed", e);
            throw new RuntimeException(e);
        }
    }
}
