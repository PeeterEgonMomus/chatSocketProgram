package org.example.chat.Client.command;

import org.example.chat.Client.connection.FramedChatConnection;

public interface CommandProcessor {
    void start(FramedChatConnection connection);
    void stop();
}
