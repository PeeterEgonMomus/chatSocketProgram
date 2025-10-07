package org.example.chat.security;

import org.example.chat.util.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class AESEncryption {
    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int AES_KEY_BITS = 256;
    private static final int IV_LENGTH = 12; // 96 bits recommended for GCM
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private AESEncryption() {}

    public static String generateKeyBase64() {
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            kgen.init(AES_KEY_BITS, RANDOM);
            SecretKey key = kgen.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            Logger.error("Failed to generate AES key", e);
            throw new RuntimeException(e);
        }
    }

    // returns base64(nonce || ciphertext)
    public static String encryptWithKeyBase64(String base64Key, String plain) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] ct = cipher.doFinal(plain.getBytes());

            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);

            String outB64 = Base64.getEncoder().encodeToString(out);
            Logger.debug("AES encrypt -> " + outB64.substring(0, Math.min(60, outB64.length())) + "...");
            return outB64;
        } catch (Exception e) {
            Logger.error("AES encrypt failed", e);
            throw new RuntimeException(e);
        }
    }

    // expects base64(nonce || ciphertext)
    public static String decryptWithKeyBase64(String base64Key, String base64NonceCipher) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            byte[] all = Base64.getDecoder().decode(base64NonceCipher);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, iv.length);
            byte[] ct = new byte[all.length - iv.length];
            System.arraycopy(all, iv.length, ct, 0, ct.length);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            byte[] plain = cipher.doFinal(ct);
            String out = new String(plain);
            Logger.debug("AES decrypt -> \"" + (out.length() > 60 ? out.substring(0,60) + "..." : out) + "\"");
            return out;
        } catch (Exception e) {
            Logger.error("AES decrypt failed", e);
            throw new RuntimeException(e);
        }
    }
}
