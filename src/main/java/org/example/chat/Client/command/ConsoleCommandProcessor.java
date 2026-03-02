package org.example.chat.Client.command;

import org.example.chat.Client.ClientMessageGateway;
import org.example.chat.Client.CommandInputSource;
import org.example.chat.Client.command.strategy.CommandRegistry;
import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.util.Logger;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ConsoleCommandProcessor implements CommandProcessor {

    private final CommandInputSource inputSource;

    private final ClientMessageGateway gateway;

    private final CommandRegistry commandRegistry;

    public ConsoleCommandProcessor(
            CommandInputSource inputSource, CommandRegistry commandRegistry,
            ClientMessageGateway gateway
    ) {
        this.inputSource = inputSource;
        this.commandRegistry = commandRegistry;
        this.gateway = gateway;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "command-processor");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean running = true;

    @Override
    public void start(FramedChatConnection connection) {
        executor.submit(this::readLoop);
    }

    private void readLoop() {
        try {

            while (running) {

                System.out.print("> ");

                String line = inputSource.readLine();

                if (line == null)
                    break;

                handleInput(line.trim());
            }

        } catch (Exception e) {
            Logger.error("Command processor stopped", e);
        }
    }

    private void handleInput(String input) throws Exception {

        if (input.isEmpty()) return;

        if (commandRegistry.dispatch(input)) {
            return;
        }

        sendChat(input);
    }


    private void sendChat(String message) throws Exception {
        gateway.sendChat(message);
    }


    @Override
    public void stop() {
        running = false;
        executor.shutdownNow();
    }
}
