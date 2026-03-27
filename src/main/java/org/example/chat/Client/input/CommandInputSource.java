package org.example.chat.Client.input;

/**
 * Abstraction for any input source that provides user commands.
 *
 * Responsibilities:
 * - Provide lines of input to the client runtime
 *
 * Design:
 * - Decouples input mechanism from business logic
 * - Enables alternative input sources (GUI, network, test mocks, etc.)
 *
 * Why this abstraction matters:
 * - The client does NOT depend directly on System.in
 * - Allows unit testing by injecting a fake input source
 * - Makes the client adaptable to future UI implementations
 *
 * Example implementations:
 * - SystemConsoleInputSource (console-based input)
 * - GUIInputSource (future Swing/JavaFX UI)
 * - ScriptedInputSource (automated testing)
 */
public interface CommandInputSource {

    /**
     * Reads the next line of user input.
     *
     * @return User input string (may be null if stream closes)
     * @throws Exception If input reading fails
     */
    String readLine() throws Exception;
}