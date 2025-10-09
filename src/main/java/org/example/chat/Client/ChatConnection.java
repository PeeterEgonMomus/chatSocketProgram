package org.example.chat.Client;// package org.example.chat;

import java.io.Closeable;
import java.io.IOException;

public interface ChatConnection extends Closeable {
    void send(String message) throws IOException;
    String receive() throws IOException;
}
