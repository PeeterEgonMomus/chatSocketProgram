package org.example.chat.commands;

import org.example.chat.Command;
import org.example.chat.ClientHandler;
import org.example.chat.auth.*;

/**
 * Design choice:
 * Command handles orchestration, not storage logic.
 *
 * Authentication logic is delegated to:
 * - UserStore (data access)
 * - PasswordHasher (security)
 * - SessionManager (state)
 *
 * Key benefits:
 * - Separation of concerns
 * - Easier to evolve authentication independently
 */
public class LoginCommand implements Command {

    private final UserStore userStore;
    private final UserSessionManager sessionManager;

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

                /**
                 * Design choice:
                 * ClientHandler stores minimal session identity,
                 * while SessionManager owns global session state.
                 */
                client.setUsername(username);

                client.send("Login successful! Welcome " + username);

                /**
                 * Design choice:
                 * Centralized session tracking enables:
                 * - presence features
                 * - multi-client management
                 */
                sessionManager.registerSession(username, client);

            } else {
                client.send("Invalid password.");
            }

        }, () -> client.send("User not found."));
    }
}