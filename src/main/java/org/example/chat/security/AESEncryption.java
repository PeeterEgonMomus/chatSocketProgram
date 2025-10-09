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
            Logger.debug("Encrypting with AES...");
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plainText.getBytes());

            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);

            String result = Base64.getEncoder().encodeToString(out);
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
            Logger.debug("Decrypting AES message...");
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            byte[] all = Base64.getDecoder().decode(cipherText);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, iv.length);
            byte[] ct = new byte[all.length - iv.length];
            System.arraycopy(all, iv.length, ct, 0, ct.length);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(ct);
            String text = new String(plain);
            Logger.debug("AES decryption done (plain length=" + text.length() + ")");
            return text;
        } catch (Exception e) {
            Logger.error("AES decryption failed", e);
            throw new RuntimeException(e);
        }
    }
}
