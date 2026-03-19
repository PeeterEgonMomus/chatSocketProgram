package org.example.chat.Client.gateway;

import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;

import java.nio.charset.StandardCharsets;

public final class FramedConnectionGateway implements ClientMessageGateway {

    private final FramedChatConnection connection;

    public FramedConnectionGateway(FramedChatConnection connection) {
        this.connection = connection;
    }

    @Override
    public void send(Frame frame) throws Exception {
        connection.send(frame);
    }

    @Override
    public void sendChat(String message) throws Exception {
        connection.send(
                new Frame(
                        FrameType.CHAT,
                        message.getBytes(StandardCharsets.UTF_8)
                )
        );
    }
}