package org.example.chat.commands;

import org.example.chat.Command;
import org.example.chat.ClientHandler;
import org.example.chat.CommandRegistry;

/**
 * Design choice:
 * Command introspection via registry.
 *
 * Instead of hardcoding help text,
 * we derive it from registered commands.
 *
 * Key benefits:
 * - Always up-to-date
 * - OCP: new commands automatically appear in help
 */
public class HelpCommand implements Command {

    private final CommandRegistry registry;

    public HelpCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "HELP";
    }

    @Override
    public void execute(ClientHandler client, String[] args) {

        StringBuilder sb = new StringBuilder("Available commands: ");
        sb.append(String.join(", ", registry.getCommandNames()));

        client.send(sb.toString());
    }
}