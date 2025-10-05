package org.example.chat;

import org.example.chat.security.RSAClient;
import org.example.chat.util.Logger;

import java.io.*;
import java.net.Socket;

public class ChatClient {
    private final String host;
    private final int port;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        RSAClient rsaClient = new RSAClient();

        try (Socket socket = new Socket(host, port);
             BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to chat server at " + host + ":" + port);

            // 1) read server's PUBLIC_KEY line (handshake)
            String firstLine = serverIn.readLine();
            if (firstLine != null && firstLine.startsWith("PUBLIC_KEY:")) {
                String serverPub = firstLine.substring("PUBLIC_KEY:".length()).trim();
                rsaClient.setServerPublicKeyBase64(serverPub);
                Logger.debug("Received server public key.");
            } else {
                System.err.println("Protocol error: expected PUBLIC_KEY from server first.");
                return;
            }

            // 2) send our client public key to server (plaintext)
            String clientPubBase64 = rsaClient.getPublicKeyBase64();
            serverOut.println("CLIENT_KEY:" + clientPubBase64);
            Logger.debug("Sent CLIENT_KEY to server.");

            // Thread to read messages from server and decrypt them using client's private key
            Thread readerThread = new Thread(() -> {
                String line;
                try {
                    while ((line = serverIn.readLine()) != null) {
                        // messages from server to this client should be encrypted with client's public key
                        try {
                            String decrypted = rsaClient.decrypt(line);
                            System.out.println(decrypted);
                        } catch (RuntimeException e) {
                            // Could be plaintext or error; show raw for debugging
                            Logger.error("Failed to decrypt server message; showing raw", e);
                            System.out.println("(raw) " + line);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Connection closed.");
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            // Main loop to send user input to server (encrypt with server public key)
            String input;
            while ((input = userIn.readLine()) != null) {
                String encrypted = rsaClient.encryptForServer(input);
                serverOut.println(encrypted);
            }

        } catch (IOException e) {
            System.err.println("Unable to connect to server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;
        if (args.length >= 1) host = args[0];
        if (args.length >= 2) port = Integer.parseInt(args[1]);

        new ChatClient(host, port).start();
    }
}
