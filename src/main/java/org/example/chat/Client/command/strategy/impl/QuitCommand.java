package org.example.chat.Client.command.strategy.impl;

import org.example.chat.Client.command.strategy.CommandStrategy;


/**
 * Command strategy that handles the "/quit" command.
 *
 * Responsibilities:
 * - Detect "/quit"
 * - Execute shutdown logic via injected Runnable
 *
 * Design:
 * - Uses dependency injection for shutdown behavior
 * - Does NOT directly depend on connection implementation
 * - Promotes loose coupling
 *
 * Why Runnable?
 * - Makes this command reusable
 * - Allows caller to define shutdown behavior
 */
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