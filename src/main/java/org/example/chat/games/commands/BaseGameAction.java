package org.example.chat.games.commands;

import org.example.chat.ClientHandler;

public abstract class BaseGameAction implements GameAction {

    protected boolean requireArgs(ClientHandler client, String[] args, int min, String usage) {
        if (args.length < min) {
            client.send(usage);
            return false;
        }
        return true;
    }
}