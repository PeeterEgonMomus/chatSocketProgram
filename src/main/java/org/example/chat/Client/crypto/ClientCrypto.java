package org.example.chat.Client.crypto;

public interface ClientCrypto {
    // Set/receive server public key
    void setServerPublicKeyBase64(String serverPubBase64) throws Exception;

    // client public key in base64 (for server to encrypt to client if needed)
    String getPublicKeyBase64();

    // generate and store an AES key (returns base64 of the key)
    String generateAESKeyBase64() throws Exception;

    // mark AES ready locally (called when server replies ACK)
    void markAESReady();

    boolean isAESReady();

    // encrypt AES key (base64) for server using server public RSA key
    String encryptForServerRSA(String dataBase64) throws Exception;

    // encrypt message to server (uses AES if ready, otherwise RSA fallback)
    String encryptForServer(String message) throws Exception;

    // decrypt message from server (assumes AES ready)
    String decryptFromServer(String payload) throws Exception;

    // 🔐 Encrypt raw bytes for server (AES only)
    byte[] encryptBytesForServer(byte[] data) throws Exception;

    // 🔓 Decrypt raw bytes from server
    byte[] decryptBytesFromServer(byte[] data) throws Exception;
}
