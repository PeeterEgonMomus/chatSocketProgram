package org.example.chat.Client.command;

import org.example.chat.Client.gateway.ClientMessageGateway;
import org.example.chat.Client.input.CommandInputSource;
import org.example.chat.Client.command.strategy.CommandRegistry;
import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.util.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Console-Based Command Processor.
 *
 * Responsibility:
 * - Continuously reads user input from console.
 * - Dispatches commands via CommandRegistry.
 * - Falls back to sending plain chat messages.
 *
 * Architecture Role:
 * Input Layer of the Client.
 *
 * Flow:
 * Console Input
 *     ↓
 * CommandRegistry (if command)
 *     ↓
 * ClientMessageGateway (if chat)
 *
 * Design Characteristics:
 * - Runs on its own dedicated background thread.
 * - Non-blocking relative to network thread.
 * - Clean separation between input reading and message sending.
 *
 * It does NOT:
 * - Handle encryption
 * - Manage sockets
 * - Interpret protocol frames
 *
 * Patterns:
 * - Strategy (CommandRegistry)
 * - Gateway (ClientMessageGateway)
 * - Single-threaded Executor for isolation
 */
public final class ConsoleCommandProcessor implements CommandProcessor {

    private final CommandInputSource inputSource;
    private final ClientMessageGateway gateway;
    private final CommandRegistry commandRegistry;

    /**
     * Dependencies are injected.
     *
     * This keeps input handling decoupled from transport and protocol.
     */
    public ConsoleCommandProcessor(
            CommandInputSource inputSource,
            CommandRegistry commandRegistry,
            ClientMessageGateway gateway
    ) {
        this.inputSource = inputSource;
        this.commandRegistry = commandRegistry;
        this.gateway = gateway;
    }

    /**
     * Dedicated single-thread executor.
     *
     * Ensures:
     * - Ordered input processing
     * - Isolation from networking threads
     * - Controlled shutdown behavior
     */
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "command-processor");
                t.setDaemon(true);
                return t;
            });

    private volatile boolean running = true;

    /**
     * Starts the input read loop.
     *
     * Connection parameter is passed for future extensibility,
     * though this implementation relies on the gateway abstraction.
     */
    @Override
    public void start(FramedChatConnection connection) {
        executor.submit(this::readLoop);
    }

    /**
     * Continuous blocking read loop.
     *
     * Reads input line-by-line and forwards for handling.
     */
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

    /**
     * Handles a single line of input.
     *
     * Order of evaluation:
     * 1. Ignore empty input
     * 2. Try dispatching as command
     * 3. Otherwise treat as chat message
     */
    private void handleInput(String input) throws Exception {

        if (input.isEmpty()) return;

        /*
         * CommandRegistry returns true if handled.
         */
        if (commandRegistry.dispatch(input)) {
            return;
        }

        sendChat(input);
    }

    /**
     * Sends a regular chat message via the message gateway.
     *
     * The gateway handles framing and encryption.
     */
    private void sendChat(String message) throws Exception {
        gateway.sendChat(message);
    }

    /**
     * Gracefully stops the processor.
     *
     * - Stops read loop
     * - Interrupts executor thread
     */
    @Override
    public void stop() {
        running = false;
        executor.shutdownNow();
    }
}