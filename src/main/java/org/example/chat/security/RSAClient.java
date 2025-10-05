package org.example.chat.security;

import org.example.chat.util.Logger;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAClient {
    private final KeyPair keyPair;
    private PublicKey serverPublicKey; // server public key (set after handshake)

    public RSAClient() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            this.keyPair = gen.generateKeyPair();
            Logger.debug("Client RSA keypair generated.");
        } catch (Exception e) {
            Logger.error("Failed to generate client RSA keys", e);
            throw new RuntimeException(e);
        }
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    public void setServerPublicKeyBase64(String base64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            serverPublicKey = kf.generatePublic(spec);
            Logger.debug("Client stored server public key.");
        } catch (Exception e) {
            Logger.error("Failed to set server public key", e);
            throw new RuntimeException(e);
        }
    }

    // encrypt with server public key (used for client -> server messages)
    public String encryptForServer(String plain) {
        try {
            if (serverPublicKey == null) throw new IllegalStateException("Server public key not set");
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
            byte[] encrypted = cipher.doFinal(plain.getBytes());
            String out = Base64.getEncoder().encodeToString(encrypted);
            Logger.debug("Client encrypted for server -> " + out.substring(0, Math.min(40, out.length())) + "...");
            return out;
        } catch (Exception e) {
            Logger.error("Client encryption failed", e);
            throw new RuntimeException(e);
        }
    }

    // decrypt messages encrypted to this client using client's private key
    public String decrypt(String cipherText) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            String plain = new String(decrypted);
            Logger.debug("Client decrypted message -> " + (plain.length() > 60 ? plain.substring(0,60) + "..." : plain));
            return plain;
        } catch (Exception e) {
            Logger.error("Client decryption failed", e);
            throw new RuntimeException(e);
        }
    }
}
