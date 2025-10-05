package org.example.chat;

import org.example.chat.commands.HelpCommand;
import org.example.chat.commands.BroadcastCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatCommandsTest {

    private CommandRegistry registry;
    private HelpCommand helpCommand;
    private BroadcastCommand broadcastCommand;
    private TestClient client;
    private TestServer server;

    static class TestServer extends ChatServer {
        private String lastBroadcast;

        @Override
        public void broadcast(ClientHandler sender, String message) {
            System.out.println("[SERVER] Broadcasting: " + sender.getUsername() + ": " + message);
            lastBroadcast = sender.getUsername() + ": " + message;
        }

        public String getLastBroadcast() {
            return lastBroadcast;
        }
    }

    static class TestClient extends ClientHandler {
        private String lastMessage;
        private String username;
        private final ChatServer server;

        TestClient(ChatServer server) {
            super(null, server);
            this.server = server;
            System.out.println("[CLIENT] TestClient created");
        }

        @Override
        public void send(String message) {
            System.out.println("[CLIENT] Received message: " + message);
            this.lastMessage = message;
        }

        @Override
        public void setUsername(String username) {
            this.username = username;
            System.out.println("[CLIENT] Username set to " + username);
        }

        @Override
        public String getUsername() {
            return username;
        }

        public String getLastMessage() {
            return lastMessage;
        }
    }

    @BeforeEach
    void setup() {
        System.out.println("\n===== Setting up test environment =====");
        registry = new CommandRegistry();
        helpCommand = new HelpCommand(registry);
        broadcastCommand = new BroadcastCommand();

        server = new TestServer();
        client = new TestClient(server);
        client.setUsername("alice");

        registry.register(helpCommand);
        registry.register(broadcastCommand);

        System.out.println("[SETUP COMPLETE]");
    }

    @Test
    void helpListsAllCommands() {
        System.out.println("\n--- Running test: helpListsAllCommands ---");
        helpCommand.execute(client, new String[]{});
        String msg = client.getLastMessage();

        System.out.println("[ASSERT] Client last message: " + msg);
        assertNotNull(msg);
        assertTrue(msg.contains("HELP"));
        assertTrue(msg.contains("BROAD"));
    }

    @Test
    void broadcastSendsMessage() {
        System.out.println("\n--- Running test: broadcastSendsMessage ---");
        broadcastCommand.execute(client, new String[]{"Hello", "world!"});

        String result = server.getLastBroadcast();
        System.out.println("[ASSERT] Server last broadcast: " + result);

        assertEquals("alice: Hello world!", result);
    }

    @Test
    void broadcastRequiresAuth() {
        System.out.println("\n--- Running test: broadcastRequiresAuth ---");
        boolean requiresAuth = broadcastCommand.requiresAuth();
        System.out.println("[ASSERT] BROAD requiresAuth = " + requiresAuth);
        assertTrue(requiresAuth);
    }
}
