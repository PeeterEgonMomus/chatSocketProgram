package org.example.chat.Client.crypto;

import org.example.chat.util.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class ClientEncryption implements ClientCrypto {
    private PublicKey serverPublicKey;
    private final KeyPair clientKeyPair; // RSA keys
    private SecretKey aesKey; // AES session key
    private boolean aesReady = false;
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int GCM_IV_LENGTH = 12;   // bytes

    public ClientEncryption() throws NoSuchAlgorithmException {
        Logger.info("Generating client RSA key pair...");
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        clientKeyPair = keyGen.generateKeyPair();
        Logger.info("Client RSA key pair generated successfully.");
    }

    @Override
    public void setServerPublicKeyBase64(String serverPubBase64) throws Exception {
        Logger.debug("Setting server public key (Base64 length=" + serverPubBase64.length() + ")");
        byte[] decoded = Base64.getDecoder().decode(serverPubBase64);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        serverPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
        Logger.info("Server public key successfully set.");
    }

    @Override
    public String getPublicKeyBase64() {
        String pub = Base64.getEncoder().encodeToString(clientKeyPair.getPublic().getEncoded());
        Logger.debug("Client public key exported (Base64 length=" + pub.length() + ")");
        return pub;
    }

    @Override
    public String generateAESKeyBase64() throws Exception {
        Logger.info("Generating AES-256 session key...");
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        aesKey = keyGen.generateKey();
        String base64 = Base64.getEncoder().encodeToString(aesKey.getEncoded());
        Logger.debug("AES key generated (Base64 length=" + base64.length() + ")");
        return base64;
    }

    @Override
    public void markAESReady() {
        aesReady = true;
        Logger.info("AES session is now marked as ready.");
    }

    @Override
    public boolean isAESReady() {
        return aesReady;
    }

    SecretKey getAESKey() {
        return aesKey;
    }

    @Override
    public String encryptForServerRSA(String dataBase64) throws Exception {
        if (serverPublicKey == null) {
            Logger.error("Server public key not set before RSA encryption", new IllegalStateException());
            throw new IllegalStateException("Server public key not set");
        }
        Logger.debug("Encrypting AES key with server RSA public key...");
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
        byte[] encrypted = cipher.doFinal(Base64.getDecoder().decode(dataBase64));
        String out = Base64.getEncoder().encodeToString(encrypted);
        Logger.debug("AES key encrypted via RSA (length=" + out.length() + ")");
        return out;
    }

    @Override
    public byte[] encryptBytesForServer(byte[] data) throws Exception {
        if (!aesReady) {
            Logger.error("AES not ready for byte encryption", new IllegalStateException());
            throw new IllegalStateException("AES not ready for byte encryption");
        }

        Logger.debug("Encrypting byte payload with AES/GCM | plaintext length=" + data.length);

        // Generate IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        Logger.debug("Generated IV for AES/GCM (length=" + iv.length + ")");

        // Initialize Cipher
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);

        // Encrypt
        byte[] ciphertext = cipher.doFinal(data);
        Logger.debug("AES/GCM encryption done | ciphertext length=" + ciphertext.length);

        // Prepend IV
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        Logger.debug("Final encrypted payload length (IV + ciphertext)=" + combined.length);

        return combined;
    }

    @Override
    public byte[] decryptBytesFromServer(byte[] data) throws Exception {
        if (!aesReady) {
            Logger.error("AES not ready for byte decryption", new IllegalStateException());
            throw new IllegalStateException("AES not ready for byte decryption");
        }

        Logger.debug("Decrypting byte payload with AES/GCM | total length=" + data.length);

        // Extract IV and ciphertext
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertext = new byte[data.length - GCM_IV_LENGTH];
        System.arraycopy(data, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(data, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        Logger.debug("Extracted IV length=" + iv.length + " | ciphertext length=" + ciphertext.length);

        // Initialize Cipher
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);

        // Decrypt
        byte[] decrypted = cipher.doFinal(ciphertext);
        Logger.debug("AES/GCM decryption done | plaintext length=" + decrypted.length);

        return decrypted;
    }

    @Override
    public String encryptForServer(String message) throws Exception {
        if (aesReady) {
            Logger.debug("Encrypting text message with AES/GCM | length=" + message.length());
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);
            byte[] ciphertext = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
            Logger.debug("AES/GCM encryption done | ciphertext length=" + ciphertext.length);

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            String out = Base64.getEncoder().encodeToString(combined);
            Logger.debug("AES encrypted message Base64 length=" + out.length());
            return out;
        } else {
            Logger.debug("AES not ready, using RSA fallback for message");
            return encryptForServerRSA(Base64.getEncoder().encodeToString(message.getBytes()));
        }
    }

    @Override
    public String decryptFromServer(String payload) throws Exception {
        if (!aesReady) throw new IllegalStateException("AES not ready");
        Logger.debug("Decrypting text message from server using AES/GCM | Base64 length=" + payload.length());

        byte[] combined = Base64.getDecoder().decode(payload);
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);

        String plain = new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        Logger.debug("AES/GCM decryption done | plaintext length=" + plain.length());
        return plain;
    }



}
