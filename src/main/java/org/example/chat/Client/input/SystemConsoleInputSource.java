package org.example.chat.Client.input;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Console-based implementation of CommandInputSource.
 *
 * Reads user input from standard input (System.in).
 *
 * Implementation Details:
 * - Wraps System.in with InputStreamReader
 * - Uses BufferedReader for efficient line-based reading
 *
 * Design Benefits:
 * - Keeps System.in isolated to this class
 * - Prevents console dependency from leaking into client runtime
 *
 * Threading Note:
 * - readLine() is blocking
 * - Should be called from a dedicated input thread
 */
public class SystemConsoleInputSource implements CommandInputSource {

    private final BufferedReader reader =
            new BufferedReader(new InputStreamReader(System.in));

    @Override
    public String readLine() throws Exception {
        return reader.readLine();
    }
}