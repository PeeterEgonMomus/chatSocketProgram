package org.example.chat.Client.command.strategy;

/**
 * Strategy interface for handling user-entered commands.
 *
 * Each implementation represents one executable client command
 * (e.g., /quit, /sendfile, /accept, /reject).
 *
 * Responsibilities:
 * - Decide whether the strategy supports a given user input
 * - Execute command-specific logic
 *
 * Design:
 * - Implements the Strategy Pattern
 * - Enables Open/Closed Principle:
 *      New commands can be added without modifying existing logic
 *
 * Execution Model:
 *   1. User types input
 *   2. Registry iterates strategies
 *   3. First strategy that supports(input) is executed
 */
public interface CommandStrategy {

    /**
     * Determines whether this strategy can handle the given input.
     *
     * @param input Raw user input
     * @return true if this strategy should execute
     */
    boolean supports(String input);

    /**
     * Executes the command logic.
     *
     * @param input Raw user input
     * @throws Exception If command execution fails
     */
    void execute(String input) throws Exception;
}