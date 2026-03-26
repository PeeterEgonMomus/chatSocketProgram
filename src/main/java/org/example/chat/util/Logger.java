package org.example.chat.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Design choice:
 * Lightweight centralized logging utility.
 *
 * This class provides:
 * - Thread-safe logging
 * - Console + file output
 * - Simple severity levels (INFO, DEBUG, ERROR)
 * - Automatic startup/shutdown markers
 *
 * ---------------------------------------------------------
 * Architectural Role
 * ---------------------------------------------------------
 *
 * Logger is an Infrastructure Utility.
 *
 * It is intentionally:
 * - Static
 * - Globally accessible
 * - Stateless (except file handle)
 *
 * This keeps logging simple and dependency-free.
 *
 * ---------------------------------------------------------
 * Thread Safety Strategy
 * ---------------------------------------------------------
 *
 * A single LOCK object guards:
 * - Console writes
 * - File writes
 * - Stack traces
 *
 * This prevents:
 * - Interleaved log lines
 * - Corrupted file output
 *
 * ---------------------------------------------------------
 * Configuration Strategy
 * ---------------------------------------------------------
 *
 * Log path resolution order:
 * 1. System property: chat.log.path
 * 2. Environment variable: CHAT_LOG_PATH
 * 3. Default: ./chat.log
 *
 * This allows flexible deployment configuration.
 *
 * ---------------------------------------------------------
 * Trade-offs:
 * ---------------------------------------------------------
 *
 * Pros:
 * - Zero dependencies
 * - Easy to use
 * - Predictable
 *
 * Cons:
 * - No log rotation
 * - No async logging
 * - No log levels filtering
 *
 * Suitable for:
 * - Small server
 * - Learning architecture
 * - Controlled environment
 */
public class Logger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Object LOCK = new Object();

    private static final PrintWriter fileWriter;
    private static final String logPath;

    static {
        String path = System.getProperty("chat.log.path");
        if (path == null || path.isBlank()) path = System.getenv("CHAT_LOG_PATH");
        if (path == null || path.isBlank()) path = "chat.log";
        logPath = path;

        PrintWriter w = null;
        try {
            OutputStream os = new FileOutputStream(logPath, true); // append mode
            w = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8)), true);
            w.println();
            w.println("==== LOG START " + LocalDateTime.now() + " ====");
            w.flush();
        } catch (Exception e) {
            System.out.println("[Logger] Failed to open log file '" + logPath + "': " + e.getMessage());
            w = null;
        }
        fileWriter = w;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            synchronized (LOCK) {
                if (fileWriter != null) {
                    fileWriter.println("==== LOG STOP " + LocalDateTime.now() + " ====");
                    fileWriter.flush();
                    fileWriter.close();
                }
            }
        }));
    }

    public static void info(String message) {
        log("INFO", message);
    }

    public static void debug(String message) {
        log("DEBUG", message);
    }

    /**
     * Logs an error message with optional exception.
     */
    public static void error(String message, Exception e) {
        if (e == null) {
            // just log the message
            log("ERROR", message);
            return;
        }

        // log message + exception message
        log("ERROR", message + " - " + e.getMessage());

        // print stack trace to both console and file
        synchronized (LOCK) {
            e.printStackTrace(System.out);
            if (fileWriter != null) {
                e.printStackTrace(fileWriter);
                fileWriter.flush();
            }
        }
    }

    /**
     * Convenience overload when no exception is available.
     */
    public static void error(String message) {
        error(message, null);
    }

    private static void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String line = String.format("[%s] [%s] %s", timestamp, level, message);
        synchronized (LOCK) {
            System.out.println(line);
            if (fileWriter != null) {
                fileWriter.println(line);
                fileWriter.flush();
            }
        }
    }

    /** Returns the effective log file path (useful for diagnostics). */
    public static String getLogPath() {
        return logPath;
    }
}
