package org.example.chat.Client.connection;

import org.example.chat.Client.protocol.ClientCipher;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameDecoder;
import org.example.chat.protocol.FrameEncoder;
import org.example.chat.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public final class FramedChatConnection implements FramedConnection, AutoCloseable {

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private ClientCipher cipher;

    private final Object sendLock = new Object();

    public FramedChatConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
    }

    public void setCipher(ClientCipher cipher) {
        this.cipher = cipher;
    }

    public void send(Frame frame) throws Exception {

        synchronized (sendLock) {  // ⭐ LOCK EVERYTHING

            byte[] payload = frame.getPayload();

            if (cipher != null) {
                payload = cipher.encrypt(frame.getType(), payload);
            }

            Frame encryptedFrame = new Frame(frame.getType(), payload);

            Logger.debug("[FRAME →] type=" + frame.getType() +
                    ", length=" + payload.length);

            FrameEncoder.write(encryptedFrame, out);
        }
    }

    public Frame receive() throws Exception {
        Frame frame = FrameDecoder.read(in);
        if (frame == null) return null;

        Logger.debug("[FRAME ←] type=" + frame.getType() + ", length=" + frame.getPayload().length);

        byte[] payload = frame.getPayload();
        if (cipher != null) {
            try {
                payload = cipher.decrypt(frame.getType(), payload);
            } catch (Exception e) {
                Logger.error("Failed to decrypt frame of type " + frame.getType() + " | payload length=" + payload.length, e);
                throw e;
            }
        }

        return new Frame(frame.getType(), payload);
    }

    @Override
    public void close() throws Exception {
        socket.close();
    }
}
