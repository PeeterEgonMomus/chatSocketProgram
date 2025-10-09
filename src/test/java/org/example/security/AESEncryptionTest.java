package org.example.security;

import org.example.chat.security.AESEncryption;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

public class AESEncryptionTest {

    @Test
    void testEncryptDecryptConsistency() {
        String keyBase64 = AESEncryption.generateKeyBase64();
        AESEncryption aes = new AESEncryption(keyBase64);

        String original = "This is a secret message";
        String cipher = aes.encrypt(original);
        String plain = aes.decrypt(cipher);

        assertThat(plain).isEqualTo(original);
    }

    @Test
    void testEncryptionProducesDifferentCiphertextsForSameInput() {
        String keyBase64 = AESEncryption.generateKeyBase64();
        AESEncryption aes = new AESEncryption(keyBase64);
        String message = "determinism test";

        String c1 = aes.encrypt(message);
        String c2 = aes.encrypt(message);

        // AES-GCM should produce different ciphertexts (different IVs)
        assertThat(c1).isNotEqualTo(c2);
    }

    @Test
    void testDecryptWithWrongKeyFails() {
        String k1 = AESEncryption.generateKeyBase64();
        String k2 = AESEncryption.generateKeyBase64();
        AESEncryption aes1 = new AESEncryption(k1);
        AESEncryption aes2 = new AESEncryption(k2);

        String cipher = aes1.encrypt("Sensitive data");

        assertThatThrownBy(() -> aes2.decrypt(cipher))
                .hasMessageContaining("Tag mismatch");
    }

}
