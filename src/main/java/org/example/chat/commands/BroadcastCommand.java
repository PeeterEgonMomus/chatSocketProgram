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
        return true;
    }

    @Override
    public void execute(ClientHandler client, String[] args) {

        if (args.length == 0) {
            client.send("Usage: BROAD <message>");
            return;
        }

        String message = String.join(" ", args);

        client.getServer().broadcast(client, message);
    }
}
