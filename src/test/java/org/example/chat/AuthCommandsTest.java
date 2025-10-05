package org.example.chat;

import org.example.chat.auth.InMemoryUserStore;
import org.example.chat.commands.LoginCommand;
import org.example.chat.commands.RegisterCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthCommandsTest {

    private InMemoryUserStore userStore;
    private RegisterCommand registerCommand;
    private LoginCommand loginCommand;
    private TestClient client;

    // ✅ Completely decoupled TestClient
    static class TestClient extends ClientHandler {
        private String lastMessage;
        private String username;

        TestClient() {
            super(null, null); // no socket or server needed
        }

        @Override
        public void send(String message) {
            this.lastMessage = message;
            System.out.println("SERVER -> " + message);
        }

        @Override
        public void setUsername(String username) {
            this.username = username;
        }

        public String getLastMessage() {
            return lastMessage;
        }

        public String getUsername() {
            return username;
        }
    }

    @BeforeEach
    void setup() {
        // ✅ Constructor injection ensures the commands are decoupled from ChatServer
        userStore = new InMemoryUserStore();
        registerCommand = new RegisterCommand(userStore);
        loginCommand = new LoginCommand(userStore);
        client = new TestClient();
    }

    @Test
    void registerFailsWhenPasswordTooWeak() {
        registerCommand.execute(client, new String[]{"alice", "short"});
        assertTrue(client.getLastMessage().contains("Registration failed"));
    }

    @Test
    void registerAndLoginFlow() {
        registerCommand.execute(client, new String[]{"alice", "mypassword1"});
        assertEquals("Registration successful!", client.getLastMessage());

        loginCommand.execute(client, new String[]{"alice", "wrong"});
        assertEquals("Invalid password.", client.getLastMessage());

        loginCommand.execute(client, new String[]{"alice", "mypassword1"});
        assertEquals("Login successful! Welcome alice", client.getLastMessage());
        assertEquals("alice", client.getUsername());
    }

    @Test
    void registerFailsForDuplicateUser() {
        registerCommand.execute(client, new String[]{"bob", "abc123"});
        registerCommand.execute(client, new String[]{"bob", "another123"});
        assertEquals("Username already exists.", client.getLastMessage());
    }
}
