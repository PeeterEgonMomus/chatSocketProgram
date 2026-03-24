package org.example.chat.games.commands;

import org.example.chat.ClientHandler;

public interface GameAction {

    String name();

    void execute(ClientHandler client, String[] args);
}