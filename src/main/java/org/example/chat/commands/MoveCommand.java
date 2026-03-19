package org.example.chat.commands;

import org.example.chat.ClientHandler;
import org.example.chat.Command;
import org.example.chat.games.GameManager;

public class MoveCommand implements Command {

    private final GameManager gameManager;

    public MoveCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public String getName() {
        return "move";
    }

    @Override
    public void execute(ClientHandler client, String[] args) {

        if (args.length < 1) {
            client.send("Usage: /move <move>");
            return;
        }

        String move = args[0];

        gameManager.submitMove(client, move);
    }
}