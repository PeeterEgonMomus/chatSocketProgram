package org.example.chat.handshake;

import org.example.chat.ClientHandler;

public interface HandshakeService {

    void performHandshake(ClientHandler client) throws Exception;

}