package org.example.chat.files;

import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;

public interface FileTransferPeer {

    void sendEncrypted(FrameType type, byte[] payload) throws Exception;

    String getUsername();

    void send(String message) throws Exception;

}
