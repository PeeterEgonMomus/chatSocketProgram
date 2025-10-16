package org.example.chat.file;

import org.example.chat.ChatServer;
import org.example.chat.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class FileTransferServer implements Runnable {
    private final ChatServer chatServer;
    private final int port;

    public FileTransferServer(ChatServer chatServer, int basePort) {
        this.chatServer = chatServer;
        this.port = basePort + 1;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Logger.info("FileTransferServer listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                Logger.info("Incoming file transfer connection from " + socket.getRemoteSocketAddress());
                new Thread(new FileTransferHandler(socket, chatServer)).start();
            }
        } catch (IOException e) {
            Logger.error("FileTransferServer error", e);
        }
    }
}
