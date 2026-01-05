package org.example.chat.Client;

import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameDecoder;
import org.example.chat.protocol.FrameEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class FramedChatConnection implements AutoCloseable {
    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;

    public FramedChatConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
    }

    public void send(Frame frame) throws IOException {
        FrameEncoder.write(frame, out);
    }

    public Frame receive() throws IOException {
        return FrameDecoder.read(in);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
