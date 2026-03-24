package org.example.chat.commands;

import org.example.chat.ClientHandler;
import org.example.chat.Command;
import org.example.chat.games.commands.GameAction;
import org.example.chat.games.commands.GameActionRegistry;

/**
 * Design choice:
 * This class acts only as an entry point (router),
 * delegating behavior to GameAction implementations.
 *
 * This removes the switch statement (OCP violation)
 * and allows new actions without modifying this class.
 */
public class GameCommand implements Command {

    private final GameActionRegistry registry;

    public GameCommand(GameActionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "game";
    }

    @Override
    public void execute(ClientHandler client, String[] args) {

        if (args.length < 1) {
            client.send("Usage: /game <action> ...");
            return;
        }

        String actionName = args[0];

        GameAction action = registry.get(actionName);

        if (action == null) {
            client.send("Unknown game command.");
            return;
        }

        action.execute(client, args);
    }
}