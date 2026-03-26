package org.example.chat.security;

import org.example.chat.util.Logger;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


/**
 * Design choice:
 * Implements RSA asymmetric encryption.
 *
 * Responsibilities:
 * - Generate server RSA key pair (2048-bit)
 * - Decrypt data using private key
 * - Encrypt data using public key
 * - Export public key for handshake
 *
 * Primary Usage:
 * - Secure AES key exchange during handshake
 *
 * It does NOT:
 * - Encrypt regular session traffic
 * - Manage client sessions
 *
 * Architectural Role:
 * - Asymmetric bootstrap mechanism
 * - Trust establishment layer
 *
 * Security Notes:
 * - 2048-bit key strength
 * - Uses Base64 for safe transport
 *
 * Logging:
 * - Logs key generation
 * - Logs encryption/decryption operations
 * - Logs failure scenarios
 */
public class RSAEncryption implements EncryptionStrategy {
    private final KeyPair keyPair;

    public RSAEncryption() {
        try {
            Logger.info("Generating RSA key pair (2048 bits)...");
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            this.keyPair = gen.generateKeyPair();
            Logger.info("RSA key pair generated successfully.");
        } catch (Exception e) {
            Logger.error("Failed to initialize RSA", e);
            throw new RuntimeException("Failed to initialize RSA", e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        try {
            Logger.debug("Decrypting RSA message (Base64 length=" + cipherText.length() + ")");
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            String plain = new String(decrypted);
            Logger.debug("RSA decrypt successful (plain length=" + plain.length() + ")");
            return plain;
        } catch (Exception e) {
            Logger.error("RSA decryption failed", e);
            throw new RuntimeException("RSA decryption failed", e);
        }
    }

    @Override
    public String encrypt(String plainText) {
        try {
            Logger.debug("Encrypting with RSA public key...");
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            String out = Base64.getEncoder().encodeToString(encrypted);
            Logger.debug("RSA encryption done (output length=" + out.length() + ")");
            return out;
        } catch (Exception e) {
            Logger.error("RSA encryption failed", e);
            throw new RuntimeException("RSA encryption failed", e);
        }
    }

    public String encryptWithPublicKey(String plainText, String base64PublicKey) {
        try {
            Logger.debug("Encrypting with client public key (len=" + base64PublicKey.length() + ")");
            byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey pub = kf.generatePublic(spec);

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pub);
            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            String out = Base64.getEncoder().encodeToString(encrypted);
            Logger.debug("RSA encryptWithPublicKey done (output length=" + out.length() + ")");
            return out;
        } catch (Exception e) {
            Logger.error("RSA encryption with public key failed", e);
            throw new RuntimeException("RSA encryption with public key failed", e);
        }
    }

    public byte[] decryptToBytes(String base64CipherText) {
        try {
            Logger.debug("Decrypting RSA to bytes...");
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decoded = Base64.getDecoder().decode(base64CipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            Logger.debug("RSA decryptToBytes successful (bytes=" + decrypted.length + ")");
            return decrypted;
        } catch (Exception e) {
            Logger.error("RSA decryptToBytes failed", e);
            throw new RuntimeException("RSA decryptToBytes failed", e);
        }
    }

    public String getPublicKeyBase64() {
        String pub = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        Logger.debug("RSA public key exported (Base64 length=" + pub.length() + ")");
        return pub;
    }
}
