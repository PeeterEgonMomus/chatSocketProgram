package org.example.chat.Client.protocol;

import org.example.chat.protocol.FrameType;

/**
 * ClientCipher defines a cipher abstraction for per-frame encryption and decryption.
 *
 * Responsibilities:
 * - Encrypt frames before sending
 * - Decrypt frames after receiving
 *
 * Architecture Role:
 * - Allows ClientRuntime / FramedChatConnection to remain agnostic of underlying crypto
 * - Delegates actual AES or handshake logic to ClientCrypto implementations
 */
public interface ClientCipher {

    byte[] encrypt(FrameType type, byte[] payload) throws Exception;

    byte[] decrypt(FrameType type, byte[] payload) throws Exception;

}