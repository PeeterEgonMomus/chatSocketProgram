package org.example.chat.Client.command.strategy.impl;

import org.example.chat.Client.command.strategy.CommandStrategy;

public final class QuitCommand implements CommandStrategy {

    private final Runnable shutdownHook;

    public QuitCommand(Runnable shutdownHook) {
        this.shutdownHook = shutdownHook;
    }

    @Override
    public boolean supports(String input) {
        return input.equalsIgnoreCase("/quit");
    }

    @Override
    public void execute(String input) throws Exception {
        shutdownHook.run();
    }
}