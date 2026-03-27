package org.example.chat.Client.crypto;

import org.example.chat.util.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * ClientHandshakeCipher handles RSA keypair and AES key encryption
 * during the handshake phase with the server.
 *
 * Responsibilities:
 * - Generate client RSA keypair
 * - Store server RSA public key
 * - Generate AES session key for symmetric encryption
 * - Encrypt AES key using server public key
 *
 * Architecture Role:
 * - Handshake layer between client and server
 * - Establishes secure symmetric session key
 * - Precedes full ClientEncryption/AES usage
 *
 * Security Notes:
 * - RSA 2048-bit keys
 * - AES 256-bit session key
 * - AES key transmitted only after RSA encryption
 */
public class ClientHandshakeCipher implements ClientHandshakeCrypto {

    private PublicKey serverPublicKey;
    private final KeyPair clientKeyPair;

    public ClientHandshakeCipher() throws NoSuchAlgorithmException {
        Logger.info("Generating client RSA key pair...");
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        clientKeyPair = keyGen.generateKeyPair();
        Logger.info("Client RSA key pair generated.");
    }

    @Override
    public void setServerPublicKeyBase64(String serverPubBase64) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(serverPubBase64);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        serverPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
        Logger.info("Server public key set.");
    }

    @Override
    public String getClientPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(clientKeyPair.getPublic().getEncoded());
    }

    @Override
    public String generateAESKeyBase64() throws Exception {
        Logger.info("Generating AES session key...");
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey aesKey = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(aesKey.getEncoded());
    }

    @Override
    public String encryptAESKeyForServer(String aesKeyBase64) throws Exception {
        if (serverPublicKey == null) {
            throw new IllegalStateException("Server public key not set");
        }
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);

        byte[] encrypted = cipher.doFinal(Base64.getDecoder().decode(aesKeyBase64));
        return Base64.getEncoder().encodeToString(encrypted);
    }
}