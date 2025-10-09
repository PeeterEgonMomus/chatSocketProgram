package org.example.security;

import org.example.chat.security.RSAEncryption;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

public class RSAEncryptionTest {

    @Test
    void testEncryptDecryptConsistency() {
        RSAEncryption rsa = new RSAEncryption();
        String message = "RSA test";

        String cipher = rsa.encrypt(message);
        String plain = rsa.decrypt(cipher);

        assertThat(plain).isEqualTo(message);
    }

    @Test
    void testEncryptWithPublicKeyOfAnotherInstance() {
        RSAEncryption server = new RSAEncryption();
        RSAEncryption client = new RSAEncryption();

        String msg = "Cross-key test";
        String encrypted = server.encryptWithPublicKey(msg, client.getPublicKeyBase64());
        String decrypted = client.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(msg);
    }

    @Test
    void testDecryptToBytesReturnsRawData() {
        RSAEncryption rsa = new RSAEncryption();
        String plain = "hello123";

        String encrypted = rsa.encrypt(plain);
        byte[] bytes = rsa.decryptToBytes(encrypted);

        assertThat(new String(bytes)).isEqualTo(plain);
    }
}
