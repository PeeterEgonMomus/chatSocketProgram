package org.example.chat;

import org.example.chat.auth.InMemoryUserStore;
import org.example.chat.auth.UserStore;
import org.example.chat.auth.policy.CompositePasswordPolicy;
import org.example.chat.auth.policy.MinLengthPolicy;
import org.example.chat.auth.policy.MustContainNumberPolicy;
import org.example.chat.auth.policy.PasswordPolicy;
import org.example.chat.commands.BroadcastCommand;
import org.example.chat.commands.HelpCommand;
import org.example.chat.commands.LoginCommand;
import org.example.chat.commands.RegisterCommand;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {
    private final List<ClientHandler> clients = new ArrayList<>();
    private final CommandRegistry registry = new CommandRegistry();
    UserStore userStore = new InMemoryUserStore();

    public ChatServer() {
        // ✅ Build one shared password policy
        PasswordPolicy passwordPolicy = new CompositePasswordPolicy()
                .addPolicy(new MinLengthPolicy(8))
                .addPolicy(new MustContainNumberPolicy());

        // ✅ Pass it into RegisterCommand
        registry.register(new RegisterCommand(userStore, passwordPolicy));
        registry.register(new LoginCommand(userStore));
        registry.register(new HelpCommand(registry));
    }

    public void start(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Chat server started on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(socket, this);
                clients.add(client);
                new Thread(client).start();
            }
        }
    }

    public void broadcast(ClientHandler sender, String message) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.send("BROADCAST from " + sender + ": " + message);
            }
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public CommandRegistry getRegistry() {
        return registry;
    }

    public static void main(String[] args) throws IOException {
        new ChatServer().start(12345);
    }
}
