package org.example.chat;

import org.example.chat.Client.connection.FramedConnection;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;
import java.io.*;
import java.net.Socket;

public class SocketFramedConnection implements FramedConnection {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public SocketFramedConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void send(Frame frame) throws IOException {
        byte[] payload = frame.getPayload();
        out.writeByte(frame.getType().ordinal()); // type as byte
        out.writeInt(payload.length);             // payload length
        out.write(payload);                        // payload bytes
        out.flush();
    }

    @Override
    public Frame receive() throws IOException {
        try {
            int typeOrdinal = in.readByte();
            int length = in.readInt();
            byte[] payload = new byte[length];
            in.readFully(payload);
            FrameType type = FrameType.values()[typeOrdinal];
            return new Frame(type, payload);
        } catch (EOFException e) {
            return null; // client disconnected
        }
    }


    public void close() throws IOException {
        socket.close();
    }
}
