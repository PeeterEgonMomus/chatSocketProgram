package org.example.chat.Client.command.strategy;

import java.util.List;

public final class CommandRegistry {

    private final List<CommandStrategy> strategies;

    public CommandRegistry(List<CommandStrategy> strategies) {
        this.strategies = strategies;
    }

    public boolean dispatch(String input) throws Exception {

        for (CommandStrategy s : strategies) {
            if (s.supports(input)) {
                s.execute(input);
                return true;
            }
        }

        return false;
    }
}