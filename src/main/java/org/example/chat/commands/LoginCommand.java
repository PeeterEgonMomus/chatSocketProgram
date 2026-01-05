package org.example.chat.commands;

import org.example.chat.Command;
import org.example.chat.ClientHandler;
import org.example.chat.auth.*;

public class LoginCommand implements Command {
    private final UserStore userStore;
    private final UserSessionManager sessionManager; // ✅ New

    public LoginCommand(UserStore userStore, UserSessionManager sessionManager) {
        this.userStore = userStore;
        this.sessionManager = sessionManager;
    }

    @Override
    public String getName() {
        return "LOGIN";
    }

    @Override
    public void execute(ClientHandler client, String[] args) {
        if (args.length < 2) {
            client.send("Usage: LOGIN <username> <password>");
            return;
        }

        String username = args[0];
        String password = args[1];

        userStore.getUser(username).ifPresentOrElse(user -> {
            if (PasswordHasher.verify(password, user.getSalt(), user.getPasswordHash())) {
                client.setUsername(username);
                client.send("Login successful! Welcome " + username);

                // ✅ Create or update the user session
                UserSession session = new UserSession(username, client);
                sessionManager.registerSession(username, client);

                // Optional: notify all clients about the new online user
                // sessionManager.broadcastOnlineUsers();

            } else {
                client.send("Invalid password.");
            }
        }, () -> client.send("User not found."));
    }
}
