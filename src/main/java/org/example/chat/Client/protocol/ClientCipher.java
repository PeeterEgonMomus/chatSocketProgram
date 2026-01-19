package org.example.chat.Client.protocol;

import org.example.chat.protocol.FrameType;

public interface ClientCipher {

    byte[] encrypt(FrameType type, byte[] payload) throws Exception;

    byte[] decrypt(FrameType type, byte[] payload) throws Exception;

}
