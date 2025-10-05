package org.example.chat.commands;

import org.example.chat.ClientHandler;
import org.example.chat.Command;

public class BroadcastCommand implements Command {

    @Override
    public String getName() {
        return "BROAD";
    }

    @Override
    public boolean requiresAuth() {
        return true; // only logged-in users can broadcast
    }

    @Override
    public void execute(ClientHandler client, String[] args) {
        String message = String.join(" ", args);
        client.getServer().broadcast(client, message);
    }
}
