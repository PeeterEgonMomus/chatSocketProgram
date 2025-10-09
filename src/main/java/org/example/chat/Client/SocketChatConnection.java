package org.example.chat.Client;// package org.example.chat;

import java.io.*;
import java.net.Socket;

public class SocketChatConnection implements ChatConnection {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    public SocketChatConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void send(String message) {
        out.println(message);
    }

    @Override
    public String receive() throws IOException {
        return in.readLine();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
