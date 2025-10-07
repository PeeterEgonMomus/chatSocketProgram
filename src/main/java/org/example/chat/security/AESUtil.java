package org.example.chat.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class AESUtil {

    private static final String AES = "AES";
    private static final String AES_CIPHER = "AES/CBC/PKCS5Padding";

    /** Generate a random AES key (128-bit is enough for chat messages) */
    public SecretKey generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(AES);
            keyGen.init(128); // or 256 if Java supports it
            return keyGen.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate AES key", e);
        }
    }

    /** Generate a random IV (16 bytes for AES) */
    public byte[] generateIV() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /** Encode AES key as Base64 string (so it can be sent to the server via RSA) */
    public String encodeKey(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /** Decode Base64 AES key back into a SecretKey */
    public SecretKey decodeKey(String base64Key) {
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(decoded, AES);
    }

    /** Encrypt plaintext with AES key + IV, return Base64 string */
    public String encrypt(String plainText, SecretKey key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(AES_CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(cipherBytes);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    /** Decrypt Base64 ciphertext with AES key + IV */
    public String decrypt(String cipherBase64, SecretKey key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(AES_CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherBase64));
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }
}
