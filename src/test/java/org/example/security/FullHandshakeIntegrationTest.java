package org.example.security;

import org.example.chat.Client.ClientEncryption;
import org.example.chat.security.*;
import org.example.chat.ClientHandler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class FullHandshakeIntegrationTest {

    @Nested
    public class DummyClient extends ClientHandler {
        DummyClient() {
            super(null, null, null);
        }

        @Override
        public String toString() {
            return "DummyClient";
        }

        @Test
        void testFullHandshakeAndSecureMessageExchange() throws Exception {
            RSAEncryption rsa = new RSAEncryption();
            HybridEncryption hybrid = new HybridEncryption(rsa);
            EncryptionService service = new EncryptionService(hybrid);

            ClientEncryption client = new ClientEncryption();
            client.setServerPublicKeyBase64(service.getServerPublicKeyBase64());
            DummyClient handler = new DummyClient();

            // === 1. Client generates AES and sends it encrypted ===
            String clientAES = client.generateAESKeyBase64();
            String encryptedAES = client.encryptForServerRSA(clientAES);
            service.registerClientAESKey(handler, encryptedAES);
            client.markAESReady();

            // === 2. Server sends encrypted ACK using AES ===
            String ackEncrypted = service.encryptForClient(handler, "AES_OK", client.getPublicKeyBase64());
            String ackPlain = client.decryptFromServer(ackEncrypted);
            assertThat(ackPlain).isEqualTo("AES_OK");

            // === 3. Client sends encrypted message using AES ===
            String msg = "Hello secure world!";
            String encrypted = client.encryptForServer(msg);
            String decrypted = service.decryptFromClient(handler, encrypted);

            assertThat(decrypted).isEqualTo(msg);
        }
    }
}