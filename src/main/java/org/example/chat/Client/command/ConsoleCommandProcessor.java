package org.example.chat.Client.command;

import org.example.chat.Client.crypto.ClientCrypto;
import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ConsoleCommandProcessor implements CommandProcessor {

    private FramedChatConnection connection;
    private final ClientCrypto crypto;// injected dispatcher
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "command-processor");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean running = true;

    public ConsoleCommandProcessor(ClientCrypto crypto) {
        this.crypto = crypto;
    }

    @Override
    public void start(FramedChatConnection connection) {
        this.connection = connection;
        executor.submit(this::readLoop);
    }

    private void readLoop() {
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in))) {
            while (running) {
                System.out.print("> ");
                String line = reader.readLine();
                if (line == null) break;

                handleCommand(line.trim());
            }
        } catch (Exception e) {
            Logger.error("Command processor stopped", e);
        }
    }

    private void handleCommand(String input) throws Exception {
        if (input.isEmpty()) return;

        if (input.equalsIgnoreCase("/quit")) {
            running = false;
            connection.close();
            return;
        }

        if (input.startsWith("/sendfile ")) {
            String[] parts = input.split(" ", 3);
            sendFile(parts[1], parts[2]);
            return;
        }

        connection.send(new Frame(
                FrameType.CHAT,
                input.getBytes(StandardCharsets.UTF_8)
        ));
    }



    private void sendFile(String recipient, String path) throws Exception {
        File file = new File(path);
        Logger.debug("Preparing to send file: " + file.getAbsolutePath());

        byte[] fileBytes = Files.readAllBytes(file.toPath());
        Logger.debug("Read file bytes: " + fileBytes.length + " bytes");

        // Compute SHA-256 checksum of the whole file
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String checksum = Base64.getEncoder().encodeToString(digest.digest(fileBytes));
        Logger.debug("Computed file checksum (SHA-256 Base64): " + checksum);

        // Send FILE_META
        String meta = recipient + "|" + file.getName() + "|" + fileBytes.length + "|" + checksum;
        Logger.debug("Sending FILE_META: " + meta);
        connection.send(new Frame(FrameType.FILE_META, meta.getBytes(StandardCharsets.UTF_8)));

        final int CHUNK_SIZE = 4096;
        int offset = 0;
        int chunkIndex = 0;

        digest.reset(); // for streaming verification

        while (offset < fileBytes.length) {
            int len = Math.min(CHUNK_SIZE, fileBytes.length - offset);
            byte[] chunk = Arrays.copyOfRange(fileBytes, offset, offset + len);

            digest.update(chunk);

            Logger.debug(String.format(
                    "CLIENT CHUNK #%d | file offset [%d..%d] | plaintext bytes: %d",
                    chunkIndex, offset, offset + len - 1, chunk.length
            ));

            // 1️⃣ Build plaintext payload: [chunkIndex (4) | length (4) | data]
            ByteArrayOutputStream plainOut = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(plainOut);
            dos.writeInt(chunkIndex);
            dos.writeInt(len);
            dos.write(chunk);
            byte[] plainPayload = plainOut.toByteArray();

            Logger.debug(String.format(
                    "CLIENT CHUNK #%d | plaintext payload size: %d bytes",
                    chunkIndex, plainPayload.length
            ));

            // 2️⃣ Send plaintext payload (FramedChatConnection will handle encryption)
            connection.send(new Frame(FrameType.FILE_CHUNK, plainPayload));
            Logger.debug("CLIENT CHUNK #" + chunkIndex + " sent");

            offset += len;
            chunkIndex++;
        }

        // ✅ Notify server that file transfer is finished
        connection.send(new Frame(FrameType.FILE_END, new byte[0]));
        Logger.debug("Sent FILE_END for " + file.getName());

        Logger.debug("All file chunks sent for: " + file.getName());
        String streamedChecksum = Base64.getEncoder().encodeToString(digest.digest());
        Logger.debug("Streaming checksum (should match FILE_META): " + streamedChecksum);
    }





    @Override
    public void stop() {
        running = false;
        executor.shutdownNow();
    }
}
