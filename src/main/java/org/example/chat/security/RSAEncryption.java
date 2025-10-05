package org.example.chat.security;

import org.example.chat.util.Logger;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAEncryption implements EncryptionStrategy {
    private final KeyPair keyPair;

    public RSAEncryption() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            this.keyPair = gen.generateKeyPair();
            Logger.info("RSA key pair generated successfully.");
        } catch (Exception e) {
            Logger.error("Failed to initialize RSA", e);
            throw new RuntimeException("Failed to initialize RSA", e);
        }
    }

    // decrypt with server private key (messages from clients)
    @Override
    public String decrypt(String cipherText) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            String plain = new String(decrypted);
            Logger.debug("RSA decrypt -> \"" + (plain.length() > 60 ? plain.substring(0, 60) + "..." : plain) + "\"");
            return plain;
        } catch (Exception e) {
            Logger.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    // encrypt with server public key (rarely needed)
    @Override
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            String out = Base64.getEncoder().encodeToString(encrypted);
            Logger.debug("RSA encrypt (server pub) -> " + out.substring(0, Math.min(40, out.length())) + "...");
            return out;
        } catch (Exception e) {
            Logger.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    // encrypt with an arbitrary public key given as base64 (used to encrypt to clients)
    public String encryptWithPublicKey(String plainText, String base64PublicKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey pub = kf.generatePublic(spec);

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pub);
            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            String out = Base64.getEncoder().encodeToString(encrypted);
            Logger.debug("RSA encryptWithPublicKey -> " + out.substring(0, Math.min(40, out.length())) + "...");
            return out;
        } catch (Exception e) {
            Logger.error("Encryption with public key failed", e);
            throw new RuntimeException("Encryption with public key failed", e);
        }
    }

    // expose server public key in base64 so clients can obtain it
    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }
}
