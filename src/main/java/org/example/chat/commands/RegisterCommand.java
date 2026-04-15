package org.example.chat.commands;

import org.example.chat.ClientHandler;
import org.example.chat.Command;
import org.example.chat.auth.*;
import org.example.chat.auth.policy.PasswordRule;
import org.example.chat.auth.policy.PasswordPolicyException;

/**
 * Design choice:
 * Validation is externalized via PasswordRule.
 *
 * This avoids hardcoding password logic inside the command.
 *
 * Key benefits:
 * - OCP: new rules can be added without modifying this class
 * - Reusability: same rules can be used elsewhere
 */
public class RegisterCommand implements Command {

    private final UserStore userStore;
    private final PasswordRule passwordRule;
    private final PasswordEncoder passwordEncoder;

    /**
     * Design choice:
     * Dependency injection instead of internal construction.
     *
     * This allows:
     * - flexible policy configuration
     * - easier testing
     */
    public RegisterCommand(UserStore userStore,
                           PasswordRule passwordRule,
                           PasswordEncoder passwordEncoder) {
        this.userStore = userStore;
        this.passwordRule = passwordRule;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String getName() {
        return "REGISTER";
    }

    @Override
    public void execute(ClientHandler client, String[] args) {

        if (args.length < 2) {
            client.send("Usage: REGISTER <username> <password>");
            return;
        }

        String username = args[0];
        String password = args[1];

        try {
            /**
             * Design choice:
             * Validation delegated to a composable policy system.
             */
            passwordRule.validate(password);

        } catch (PasswordPolicyException e) {
            client.send("Registration failed: " + e.getMessage());
            return;
        }

        String encodedPassword = passwordEncoder.encode(password);

        boolean success = userStore.addUser(
                new User(username, encodedPassword)
        );

        client.send(success
                ? "Registration successful!"
                : "Username already exists.");
    }
}