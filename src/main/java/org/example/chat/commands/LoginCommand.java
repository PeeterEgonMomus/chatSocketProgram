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
    private final PasswordEncoder passwordEncoder;

    public LoginCommand(UserStore userStore,
                        UserSessionManager sessionManager,
                        PasswordEncoder passwordEncoder) {
        this.userStore = userStore;
        this.sessionManager = sessionManager;
        this.passwordEncoder = passwordEncoder;
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

            if (passwordEncoder.verify(password, user.getEncodedPassword())) {

                client.setUsername(username);
                client.send("Login successful! Welcome " + username);

                sessionManager.registerSession(username, client);

                // Optional progressive rehash
                if (passwordEncoder.needsRehash(user.getEncodedPassword())) {
                    String upgraded = passwordEncoder.encode(password);
                    userStore.updatePassword(username, upgraded);
                }

            } else {
                client.send("Invalid password.");
            }

        }, () -> client.send("User not found."));
    }
}