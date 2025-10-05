package org.example.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);

            String input;
            while ((input = in.readLine()) != null) {
                server.getRegistry().executeCommand(this, input);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
            server.removeClient(this);
        }
    }

    public void send(String message) {
        out.println(message);
    }

    public ChatServer getServer() {
        return server;
    }

    @Override
    public String toString() {
        return socket.getRemoteSocketAddress().toString();
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAuthenticated() {
        return username != null && !username.isEmpty();
    }
}
