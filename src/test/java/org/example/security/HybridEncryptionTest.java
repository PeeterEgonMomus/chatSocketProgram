package org.example.security;

import org.example.chat.Client.crypto.ClientEncryption;
import org.example.chat.ClientHandler;
import org.example.chat.security.AESEncryption;
import org.example.chat.security.HybridEncryption;
import org.example.chat.security.RSAEncryption;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class HybridEncryptionTest {

    private static class DummyClient extends ClientHandler {
        private final String name;
        DummyClient(String name) { super(null, null, null); this.name = name; }
        @Override public String toString() { return name; }
    }

    @Test
    void testAesRegistrationAndDecryption() throws Exception {
        RSAEncryption rsa = new RSAEncryption();
        HybridEncryption hybrid = new HybridEncryption(rsa);
        DummyClient client = new DummyClient("client1");

        // Simulate client AES key (raw 256-bit)
        String aesBase64 = AESEncryption.generateKeyBase64();

        // Encrypt AES key with server RSA public key
        ClientEncryption clientEnc = new ClientEncryption();
        clientEnc.setServerPublicKeyBase64(rsa.getPublicKeyBase64());
        String encryptedAESKey = clientEnc.encryptForServerRSA(aesBase64);

        // Register client’s AES key
        hybrid.registerClientAESKey(client, encryptedAESKey);
        assertThat(hybrid.hasAES(client)).isTrue();

        // Test encryption/decryption
        String msg = "Hello Hybrid World!";
        String enc = hybrid.encryptForClient(client, msg, clientEnc.getPublicKeyBase64());
        String dec = hybrid.decryptFromClient(client, enc);

        assertThat(dec).isEqualTo(msg);
    }
}
