package org.example.chat;

import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;

import java.io.Closeable;
import java.io.IOException;

public interface FramedConnection extends Closeable {
    void send(Frame frame) throws IOException;
    Frame receive() throws IOException;
}
