package org.example.chat.Client.crypto;

import org.example.chat.protocol.FrameType;

public interface ClientCrypto {

    byte[] encryptBytesForServer(byte[] payload, FrameType type) throws Exception;

    byte[] decryptBytesFromServer(byte[] payload, FrameType type) throws Exception;
    boolean isAESReady();
}
