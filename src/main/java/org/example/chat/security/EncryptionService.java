package org.example.chat.security;

public class EncryptionService {
    private final RSAEncryption rsa; // server-side RSA implementation

    public EncryptionService(RSAEncryption rsa) {
        this.rsa = rsa;
    }

    // decrypt ciphertext coming from client -> plaintext (server uses its private key)
    public String decrypt(String cipherText) {
        return rsa.decrypt(cipherText);
    }

    // helper: server's public key (base64) to give to clients
    public String getServerPublicKeyBase64() {
        return rsa.getPublicKeyBase64();
    }

    // encrypt message for an arbitrary public key (base64)
    public String encryptWithPublicKey(String message, String base64PublicKey) {
        return rsa.encryptWithPublicKey(message, base64PublicKey);
    }

    // optional: server-side encrypt using server key (not used for server->client)
    public String encrypt(String message) {
        return rsa.encrypt(message);
    }
}
