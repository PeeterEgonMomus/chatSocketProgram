package org.example.chat.commands;

import org.example.chat.ClientHandler;
import org.example.chat.Command;

/**
 * Design choice:
 * Thin command delegating to the server.
 *
 * Commands should not contain business logic;
 * they act as adapters between user input and domain behavior.
 *
 * Key benefits:
 * - Keeps command layer lightweight
 * - Logic stays centralized in ChatServer
 */
public class BroadcastCommand implements Command {

    @Override
    public String getName() {
        return "BROAD";
    }

    /**
     * Design choice:
     * Declarative auth requirement instead of inline checks.
     */
    @Override
    public boolean requiresAuth() {
        return true;
    }

    @Override
    public void execute(ClientHandler client, String[] args) {

        if (args.length == 0) {
            client.send("Usage: BROAD <message>");
            return;
        }

        String message = String.join(" ", args);

        /**
         * Design choice:
         * Delegation to server keeps command focused on input translation.
         */
        client.getServer().broadcast(client, message);
    }
}