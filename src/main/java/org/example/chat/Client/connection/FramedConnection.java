package org.example.chat.Client.connection;

import org.example.chat.protocol.Frame;

public interface FramedConnection {

    void send(Frame frame) throws Exception;

    Frame receive() throws Exception;
}
