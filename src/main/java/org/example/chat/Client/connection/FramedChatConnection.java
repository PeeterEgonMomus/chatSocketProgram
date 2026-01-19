package org.example.chat.Client.connection;

import org.example.chat.Client.protocol.ClientCipher;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameDecoder;
import org.example.chat.protocol.FrameEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


public final class FramedChatConnection implements FramedConnection, AutoCloseable {

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private ClientCipher cipher;

    public FramedChatConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
    }

    public void setCipher(ClientCipher cipher) {
        this.cipher = cipher;
    }

    public void send(Frame frame) throws Exception {
        byte[] payload = frame.getPayload();

        if (cipher != null) {
            payload = cipher.encrypt(frame.getType(), payload);
        }

        Frame encryptedFrame = new Frame(frame.getType(), payload);
        FrameEncoder.write(encryptedFrame, out);
    }

    public Frame receive() throws Exception {
        Frame frame = FrameDecoder.read(in);
        if (frame == null) return null;

        byte[] payload = frame.getPayload();
        if (cipher != null) {
            payload = cipher.decrypt(frame.getType(), payload);
        }

        return new Frame(frame.getType(), payload);
    }

    @Override
    public void close() throws Exception {
        socket.close();
    }
}
