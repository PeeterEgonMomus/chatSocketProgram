package org.example.chat.Client.gateway;

import org.example.chat.protocol.Frame;

public interface ClientMessageGateway {
    void send(Frame frame) throws Exception;

    void sendChat(String message) throws Exception;
}