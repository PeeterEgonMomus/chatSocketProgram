package org.example.chat;

import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameContext;
import org.example.chat.protocol.FrameType;

public interface FrameHandler {

    FrameType type();

    void handle(FrameContext ctx) throws Exception;

}