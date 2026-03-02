package org.example.chat.Client.crypto;

public interface ClientHandshakeCrypto {

    void setServerPublicKeyBase64(String serverPubBase64) throws Exception;

    String getClientPublicKeyBase64();

    String generateAESKeyBase64() throws Exception;

    String encryptAESKeyForServer(String aesKeyBase64) throws Exception;
}
