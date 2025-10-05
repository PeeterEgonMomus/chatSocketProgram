package org.example.chat.commands;

import org.example.chat.ClientHandler;
import org.example.chat.Command;
import org.example.chat.auth.PasswordHasher;
import org.example.chat.auth.User;
import org.example.chat.auth.UserStore;
import org.example.chat.auth.policy.CompositePasswordPolicy;
import org.example.chat.auth.policy.MinLengthPolicy;
import org.example.chat.auth.policy.MustContainNumberPolicy;
import org.example.chat.auth.policy.PasswordPolicy;
import org.example.chat.auth.policy.PasswordPolicyException;

public class RegisterCommand implements Command {
    private final UserStore userStore;
    private final PasswordPolicy passwordPolicy;

    // ✅ Constructor injection for password policy
    public RegisterCommand(UserStore userStore, PasswordPolicy passwordPolicy) {
        this.userStore = userStore;
        this.passwordPolicy = passwordPolicy;
    }

    // Optional fallback constructor for backward compatibility
    public RegisterCommand(UserStore userStore) {
        this(userStore,
                new CompositePasswordPolicy()
                        .addPolicy(new MinLengthPolicy(6))
                        .addPolicy(new MustContainNumberPolicy()));
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
            passwordPolicy.validate(password);
        } catch (PasswordPolicyException e) {
            client.send("Registration failed: " + e.getMessage());
            return;
        }

        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(password, salt);

        boolean success = userStore.addUser(new User(username, hash, salt));
        client.send(success ? "Registration successful!" : "Username already exists.");
    }
}
