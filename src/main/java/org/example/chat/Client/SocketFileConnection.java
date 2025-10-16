package org.example.chat.Client;

import org.example.chat.util.Logger;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Socket-backed implementation of FileTransferConnection.
 *
 * Behavior:
 *  - sendBytes() writes and flushes immediately.
 *  - receiveBytes(...) delegates to InputStream.read(...) and returns what was read (-1 on EOF).
 *  - Optionally sets a read timeout on the socket to avoid indefinite blocking (adjust to taste).
 */
public class SocketFileConnection implements FileTransferConnection {
    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;

    // configure a read timeout (ms). 0 = disable. Tune as needed.
    private static final int READ_TIMEOUT_MS = 0; // e.g. 5000 for 5s

    public SocketFileConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();

        if (READ_TIMEOUT_MS > 0) {
            try {
                socket.setSoTimeout(READ_TIMEOUT_MS);
            } catch (IOException e) {
                Logger.debug("Failed to set socket timeout: " + e.getMessage());
            }
        }
    }

    @Override
    public void sendBytes(byte[] data) throws IOException {
        out.write(data);
        out.flush();
    }

    @Override
    public int receiveBytes(byte[] buffer) throws IOException {
        try {
            return in.read(buffer);
        } catch (SocketTimeoutException e) {
            // Timeout — return 0 to indicate no bytes right now (caller can retry/loop),
            // or rethrow if you prefer. We return 0 to avoid breaking your accumulating loop.
            Logger.debug("Socket read timed out while reading into buffer: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public int receiveBytes(byte[] buffer, int offset, int length) throws IOException {
        // Basic validation
        if (buffer == null) throw new NullPointerException("buffer is null");
        if (offset < 0 || length < 0 || offset + length > buffer.length)
            throw new IndexOutOfBoundsException("Invalid offset/length");

        try {
            return in.read(buffer, offset, length);
        } catch (SocketTimeoutException e) {
            Logger.debug("Socket read timed out while reading " + length + " bytes (offset " + offset + "): " + e.getMessage());
            return 0; // treat timeout as "no bytes this iteration"
        }
    }

    /**
     * Optional helper: read exactly expected bytes (blocks until all bytes read or EOF).
     * Returns number of bytes read (expectedBytes on success) or -1 on EOF before completion.
     *
     * Not part of the interface; kept here for convenience if you want to use it in future.
     */
    public int receiveFully(byte[] buffer, int expectedBytes) throws IOException {
        int total = 0;
        while (total < expectedBytes) {
            int n = receiveBytes(buffer, total, expectedBytes - total);
            if (n == -1) {
                return -1; // EOF
            }
            // If we returned 0 because of a timeout, continue the loop and retry
            if (n == 0) continue;
            total += n;
        }
        return total;
    }

    @Override
    public void close() throws IOException {
        try {
            socket.close();
        } catch (IOException e) {
            Logger.debug("Error closing socket: " + e.getMessage());
            throw e;
        }
    }
}
