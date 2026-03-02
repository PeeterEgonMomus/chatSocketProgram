package org.example.chat.Client.crypto;

import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class ClientEncryption implements ClientCrypto {

    private final SecretKey aesKey;

    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int GCM_IV_LENGTH = 12;   // bytes

    public ClientEncryption(SecretKey aesKey) {
        if (aesKey == null) {
            throw new IllegalArgumentException("AES key cannot be null");
        }
        this.aesKey = aesKey;
        Logger.info("ClientEncryption initialized with AES session key.");
    }

    // =========================
    // Core AES + AAD Logic
    // =========================

    private byte[] encrypt(byte[] plaintext, FrameType type) throws Exception {
        Logger.debug("AES encrypt | type=" + type +
                " | plaintext length=" + plaintext.length);

        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        byte[] aad = type.name().getBytes(StandardCharsets.UTF_8);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);
        cipher.updateAAD(aad);

        byte[] ciphertext = cipher.doFinal(plaintext);

        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        Logger.debug("AES encrypt | final length=" + combined.length);
        return combined;
    }

    private byte[] decrypt(byte[] data, FrameType type) throws Exception {
        Logger.debug("AES decrypt | type=" + type +
                " | total length=" + data.length);

        if (data.length < GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Encrypted payload too short");
        }

        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertext = new byte[data.length - GCM_IV_LENGTH];

        System.arraycopy(data, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(data, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        byte[] aad = type.name().getBytes(StandardCharsets.UTF_8);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);
            cipher.updateAAD(aad);

            byte[] plain = cipher.doFinal(ciphertext);

            Logger.debug("AES decrypt | plaintext length=" + plain.length);
            return plain;

        } catch (javax.crypto.AEADBadTagException e) {
            Logger.error("❌ AES/GCM authentication failed! " +
                    "Possible key mismatch or wrong FrameType.", e);
            throw e;
        }
    }

    // =========================
    // Public API
    // =========================

    @Override
    public byte[] encryptBytesForServer(byte[] payload, FrameType type) throws Exception {
        return encrypt(payload, type);
    }

    @Override
    public byte[] decryptBytesFromServer(byte[] payload, FrameType type) throws Exception {
        return decrypt(payload, type);
    }

    @Override
    public boolean isAESReady() {
        return aesKey != null;
    }

}
