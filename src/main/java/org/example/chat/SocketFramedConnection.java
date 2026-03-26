package org.example.chat;

import org.example.chat.Client.connection.FramedConnection;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;
import java.io.*;
import java.net.Socket;

/**
 * Design choice:
 * Concrete transport adapter for framed communication over TCP sockets.
 *
 * Implements the FramedConnection abstraction using:
 * - DataInputStream
 * - DataOutputStream
 *
 * ---------------------------------------------------------
 * Architectural Role
 * ---------------------------------------------------------
 *
 * This class lives in the Transport Layer.
 *
 * Responsibilities:
 * - Serialize Frame → bytes
 * - Deserialize bytes → Frame
 *
 * It does NOT:
 * - Decrypt payload
 * - Interpret frame meaning
 * - Execute business logic
 *
 * This strict separation ensures:
 * - Transport logic is replaceable
 * - Protocol format remains consistent
 *
 * ---------------------------------------------------------
 * Wire Format:
 * ---------------------------------------------------------
 *
 * 1 byte   → FrameType ordinal
 * 4 bytes  → Payload length
 * N bytes  → Payload
 *
 * This is a simple length-prefixed binary protocol.
 *
 * ---------------------------------------------------------
 * Trade-Off:
 * ---------------------------------------------------------
 *
 * Using ordinal() is compact,
 * but tightly couples protocol to enum ordering.
 *
 * If enum order changes, protocol breaks.
 *
 * A safer alternative would be:
 * - Explicit byte id field inside FrameType.
 */
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

    /**
     * Receives a single Frame from the socket.
     *
     * Returns null if:
     * - EOF is reached (client disconnected)
     *
     * This allows the caller to gracefully terminate
     * the client loop.
     *
     * readFully ensures:
     * - Entire payload is received
     * - Prevents partial frame processing
     */
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
