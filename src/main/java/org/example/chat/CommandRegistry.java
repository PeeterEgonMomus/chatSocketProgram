package org.example.chat;

import java.util.*;

public class CommandRegistry {
    private final Map<String, Command> commands = new HashMap<>();

    public CommandRegistry() {
        // Automatically load stateless commands (via ServiceLoader)
        ServiceLoader<Command> loader = ServiceLoader.load(Command.class);
        for (Command cmd : loader) {
            register(cmd);
        }
    }

    public void register(Command command) {
        commands.put(command.getName().toUpperCase(), command);
    }

    public void executeCommand(ClientHandler client, String input) {
        if (input == null || input.isBlank()) return;

        String[] parts = input.trim().split("\\s+");
        String cmdName = parts[0].toUpperCase();
        Command command = commands.get(cmdName);

        if (command == null) {
            client.send("Unknown command: " + cmdName);
            return;
        }

        // central auth enforcement
        if (command.requiresAuth() && !client.isAuthenticated()) {
            client.send("You must log in first. Use: LOGIN <username> <password>");
            return;
        }

        String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
        command.execute(client, args);
    }

    public Set<String> getCommandNames() {
        return Collections.unmodifiableSet(commands.keySet());
    }
}
