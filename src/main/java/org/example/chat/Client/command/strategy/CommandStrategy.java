package org.example.chat.Client.command.strategy;

public interface CommandStrategy {
    boolean supports(String input);

    void execute(String input) throws Exception;
}