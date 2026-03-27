package org.example.chat.Client.crypto;

/**
 * ClientHandshakeCrypto defines the contract for asymmetric key operations
 * used during the initial handshake phase with the server.
 *
 * Responsibilities:
 * - Set server RSA public key
 * - Expose client RSA public key
 * - Generate AES session key
 * - Encrypt AES key for secure transmission to server
 *
 * Architecture Role:
 * - Used by HandshakeService to establish a secure AES session
 * - Runs before ClientEncryption/AES is ready
 */
public interface ClientHandshakeCrypto {

    void setServerPublicKeyBase64(String serverPubBase64) throws Exception;

    String getClientPublicKeyBase64();

    String generateAESKeyBase64() throws Exception;

    String encryptAESKeyForServer(String aesKeyBase64) throws Exception;
}