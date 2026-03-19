package org.example.chat.protocol;

import org.example.chat.ClientHandler;
import org.example.chat.ChatServer;
import org.example.chat.security.EncryptionService;

import java.io.*;

public class FrameContext {

    private final ClientHandler client;
    private final Frame frame;

    private byte[] decrypted;
    private DataInputStream input;

    public FrameContext(ClientHandler client, Frame frame) {
        this.client = client;
        this.frame = frame;
    }

    public ClientHandler client() {
        return client;
    }

    public ChatServer server() {
        return client.getServer();
    }

    public Frame frame() {
        return frame;
    }

    public FrameType type() {
        return frame.getType();
    }

    /* ===============================
       Decryption helpers
       =============================== */

    public byte[] payload() throws Exception {

        if (decrypted == null) {

            EncryptionService encryption =
                    server().getEncryptionService();

            decrypted =
                    encryption.decryptBytesFromClient(
                            client,
                            frame.getType(),
                            frame.getPayload()
                    );
        }

        return decrypted;
    }

    public DataInputStream input() throws Exception {

        if (input == null) {

            input = new DataInputStream(
                    new ByteArrayInputStream(payload())
            );
        }

        return input;
    }

    public String readUTF() throws Exception {
        return input().readUTF();
    }

    /* ===============================
       Send helpers
       =============================== */

    public void send(FrameType type, byte[] payload) throws Exception {
        client.sendEncrypted(type, payload);
    }

}