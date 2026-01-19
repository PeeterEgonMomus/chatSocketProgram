package org.example.chat.Client.handler;

import org.example.chat.protocol.Frame;

public interface FrameHandler {
    void handle(Frame frame) throws Exception;
}
