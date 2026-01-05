package org.example.chat.auth;

import org.example.chat.ClientHandler;
import java.net.Socket;
import java.time.LocalDateTime;

public class UserSession {
    private final String username;
    private final ClientHandler chatHandler;
    private Socket fileSocket; // optional file transfer socket
    private final LocalDateTime loginTime;

    public UserSession(String username, ClientHandler chatHandler) {
        this.username = username;
        this.chatHandler = chatHandler;
        this.loginTime = LocalDateTime.now();
    }

    public String getUsername() { return username; }
    public ClientHandler getChatHandler() { return chatHandler; }
    public LocalDateTime getLoginTime() { return loginTime; }

    public Socket getFileSocket() { return fileSocket; }
    public void setFileSocket(Socket fileSocket) { this.fileSocket = fileSocket; }
}
