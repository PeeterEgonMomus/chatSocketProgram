package org.example.chat.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple thread-safe logger that writes to stdout and to a file.
 * Log file path can be set with system property "chat.log.path" or environment var "CHAT_LOG_PATH".
 * Defaults to "./chat.log".
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
            w = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8)), true); // autoFlush true
            // write a startup marker
            w.println();
            w.println("==== LOG START " + LocalDateTime.now() + " ====");
            w.flush();
        } catch (Exception e) {
            System.out.println("[Logger] Failed to open log file '" + logPath + "': " + e.getMessage());
            w = null;
        }
        fileWriter = w;

        // ensure file is closed on shutdown
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

    public static void error(String message, Exception e) {
        log("ERROR", message + " - " + e.getMessage());
        // print stacktrace to both console and file
        synchronized (LOCK) {
            e.printStackTrace(System.out);
            if (fileWriter != null) {
                e.printStackTrace(fileWriter);
                fileWriter.flush();
            }
        }
    }

    private static void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String line = String.format("[%s] [%s] %s", timestamp, level, message);
        synchronized (LOCK) {
            // console
            System.out.println(line);
            // file
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
