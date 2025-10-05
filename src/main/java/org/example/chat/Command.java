package org.example.chat;

public interface Command {
    String getName();
    void execute(ClientHandler client, String[] args);

    /** Optional: commands that need login override this */
    default boolean requiresAuth() { return false; }

    /** Optional: for help messages */
    default String getDescription() { return ""; }
}
