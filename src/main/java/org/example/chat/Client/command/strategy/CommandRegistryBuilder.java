package org.example.chat.Client.command.strategy;

import org.example.chat.Client.command.strategy.impl.*;
import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.Client.file.FileTransferService;
import org.example.chat.Client.file.IncomingTransferRegistry;

import java.util.ArrayList;
import java.util.List;


/**
 * Factory class responsible for constructing the CommandRegistry
 * with all available command strategies.
 *
 * Responsibilities:
 * - Instantiate command strategies
 * - Inject required dependencies
 * - Return a fully configured CommandRegistry
 *
 * Design:
 * - Centralizes command wiring
 * - Prevents scattered instantiation
 * - Makes command layer easy to extend
 *
 * Architectural Role:
 *   Acts as a mini dependency injection container
 *   for the command subsystem.
 */
public final class CommandRegistryBuilder {

    public static CommandRegistry build(
            FileTransferService transferService,
            IncomingTransferRegistry registry,
            FramedChatConnection connection
    ) {

        List<CommandStrategy> strategies = new ArrayList<>();

        strategies.add(new QuitCommand(() -> {
            try {
                connection.close();
            } catch (Exception ignored) {}
        }));

        strategies.add(new SendFileCommand(transferService));
        strategies.add(new AcceptCommand(registry, connection));
        strategies.add(new RejectCommand(connection));

        return new CommandRegistry(strategies);
    }
}