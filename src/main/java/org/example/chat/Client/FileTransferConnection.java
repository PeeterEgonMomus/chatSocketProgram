package org.example.chat.Client;

import java.io.Closeable;
import java.io.IOException;

/**
 * Connection abstraction for file transfer socket.
 * Backwards-compatible: keeps receiveBytes(byte[]) and adds
 * receiveBytes(byte[], int, int) for partial reads.
 */
public interface FileTransferConnection extends Closeable {
    void sendBytes(byte[] data) throws IOException;

    /**
     * Read up to buffer.length bytes; returns number of bytes read or -1 on EOF.
     */
    int receiveBytes(byte[] buffer) throws IOException;

    /**
     * Read up to {@code length} bytes into {@code buffer} starting at {@code offset}.
     * Returns number of bytes actually read or -1 on EOF.
     *
     * This matches InputStream.read(byte[], int, int) and is what your client
     * code calls in a loop to assemble the full payload.
     */
    int receiveBytes(byte[] buffer, int offset, int length) throws IOException;
}
