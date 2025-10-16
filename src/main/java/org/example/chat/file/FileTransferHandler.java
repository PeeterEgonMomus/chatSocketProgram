package org.example.chat.file;

import org.example.chat.ChatServer;
import org.example.chat.ClientHandler;
import org.example.chat.util.Logger;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

public class FileTransferHandler implements Runnable {
    private final Socket socket;
    private final ChatServer chatServer;

    public FileTransferHandler(Socket socket, ChatServer chatServer) {
        this.socket = socket;
        this.chatServer = chatServer;
    }

    @Override
    public void run() {
        Logger.info("---- FileTransferHandler started for " + socket.getRemoteSocketAddress() + " ----");
        try (InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            // Read header line (\n-terminated)
            String header = readHeader(in);
            if (header == null || header.isEmpty()) {
                Logger.error("❌ Empty file transfer header", null);
                return;
            }
            Logger.info("📩 Received file transfer header: " + header);

            if (header.startsWith("UPLOAD:")) {
                handleUpload(header, in);
            } else if (header.startsWith("READY:")) {
                handleDownload(header, out);
            } else {
                Logger.error("❌ Unknown file transfer command: " + header, null);
            }

        } catch (IOException e) {
            Logger.error("❌ FileTransferHandler error", e);
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
            Logger.info("---- FileTransferHandler closed connection ----");
        }
    }

    // read header up to newline
    private String readHeader(InputStream in) throws IOException {
        ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') break;
            headerBuffer.write(b);
            // small safety: don't allow ridiculously large headers
            if (headerBuffer.size() > 10_000) {
                throw new IOException("Header too large");
            }
        }
        return headerBuffer.toString("UTF-8").trim();
    }

    private void handleUpload(String header, InputStream in) throws IOException {
        // Expect: UPLOAD:recipient:filename:size:checksum
        String[] parts = header.split(":");
        if (parts.length < 5) {
            Logger.error("❌ Invalid UPLOAD header (expected 5 parts): " + header, null);
            return;
        }

        String recipient = parts[1];
        String filename = parts[2];
        long declaredSize;
        try {
            declaredSize = Long.parseLong(parts[3]);
        } catch (NumberFormatException nfe) {
            Logger.error("❌ Invalid declared size in header: " + parts[3], nfe);
            return;
        }
        String checksumHex = parts[4];

        String sender = resolveSenderFromSocket();
        if ("unknown".equalsIgnoreCase(sender)) {
            Logger.error("❌ Cannot determine sender, aborting file upload", null);
            return;
        }

        Logger.info("📦 Starting file upload...");
        Logger.info("   Sender: " + sender);
        Logger.info("   Recipient: " + recipient);
        Logger.info("   Filename: " + filename);
        Logger.info("   Declared encrypted size: " + declaredSize + " bytes");
        Logger.info("   Declared checksum: " + checksumHex);

        Path outDir = Path.of("uploads");
        Files.createDirectories(outDir);
        Path dest = outDir.resolve(sender + "_" + filename);
        Logger.info("💾 File will be saved to: " + dest.toAbsolutePath());

        // Read declaredSize bytes from stream and write to file
        long totalRead = 0;
        try (OutputStream fileOut = Files.newOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            long remaining = declaredSize;

            Logger.info("⬇ Receiving file data in chunks...");
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read = in.read(buffer, 0, toRead);
                if (read == -1) {
                    Logger.error("⚠ Unexpected end of stream after " + totalRead + " bytes.", null);
                    break;
                }
                fileOut.write(buffer, 0, read);
                totalRead += read;
                remaining -= read;

                if (totalRead % (1024 * 50) < buffer.length) {
                    Logger.info("   📥 Received " + totalRead + "/" + declaredSize + " bytes...");
                }
            }
            fileOut.flush();
        } catch (IOException e) {
            Logger.error("❌ Error while receiving file data", e);
            return;
        }

        Logger.info("✅ Raw receive finished. Bytes written: " + totalRead);

        // Validate byte count
        if (totalRead != declaredSize) {
            Logger.error("❌ Size mismatch: declared=" + declaredSize + " but received=" + totalRead, null);
            // Optionally notify sender/recipient here
        }

        // Compute SHA-256 on the received bytes (encrypted bytes)
        String computedChecksum;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(dest);
            computedChecksum = HexFormat.of().formatHex(md.digest(fileBytes));
        } catch (Exception ex) {
            Logger.error("❌ Failed to compute checksum for received file", ex);
            return;
        }

        Logger.info("   Computed checksum: " + computedChecksum);

        if (computedChecksum.equalsIgnoreCase(checksumHex)) {
            Logger.info("✅ Checksum match — file integrity verified");
            // Notify recipient
            ClientHandler recipientHandler = chatServer.findClientByUsername(recipient);
            if (recipientHandler != null) {
                Logger.info("📨 Notifying recipient: " + recipient);
                recipientHandler.send("FILE_REQUEST:" + sender + ":" + filename + ":" + declaredSize);
            } else {
                Logger.error("❌ Recipient not found: " + recipient, null);
            }
        } else {
            Logger.error("❌ Checksum mismatch! expected=" + checksumHex + " computed=" + computedChecksum, null);
            // Optionally remove the corrupted file or keep for inspection
            // Files.deleteIfExists(dest);
            // Notify sender (if we can find sender client object)
            ClientHandler senderHandler = chatServer.findClientByUsername(sender);
            if (senderHandler != null) {
                senderHandler.send("FILE_ERROR:checksum_mismatch:" + filename);
            }
        }
    }

    private void handleDownload(String header, OutputStream out) throws IOException {
        // Format: READY:filename
        String[] parts = header.split(":");
        if (parts.length < 2) {
            Logger.error("❌ Invalid READY header: " + header, null);
            return;
        }

        String filename = parts[1];

        // Try to find which sender's file it is (sender is embedded in stored name: sender_filename)
        Path uploadsDir = Path.of("uploads");
        try (var stream = Files.list(uploadsDir)) {
            Path filePath = stream
                    .filter(p -> p.getFileName().toString().endsWith("_" + filename))
                    .findFirst()
                    .orElse(null);

            if (filePath == null) {
                Logger.error("❌ Requested file not found for filename: " + filename, null);
                return;
            }

            long size = Files.size(filePath);
            Logger.info("📤 Sending file to recipient...");
            Logger.info("   File: " + filePath);
            Logger.info("   Size: " + size + " bytes");

            try (InputStream fileIn = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int read;
                long totalSent = 0;

                while ((read = fileIn.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    totalSent += read;

                    if (totalSent % (1024 * 50) < buffer.length) {
                        Logger.info("   📤 Sent " + totalSent + "/" + size + " bytes...");
                    }
                }
                out.flush();
                Logger.info("✅ File sent successfully (" + totalSent + " bytes)");
            }
        }
    }


    private String resolveSenderFromSocket() {
        // This method expects ChatServer to expose getAllClients() and ClientHandler to expose getSocket() and getUsername()
        for (ClientHandler client : chatServer.getAllClients()) {
            try {
                if (client.getSocket() != null && client.getSocket().getRemoteSocketAddress() != null &&
                        client.getSocket().getRemoteSocketAddress().equals(socket.getRemoteSocketAddress())) {
                    return client.getUsername();
                }
            } catch (Exception ignored) {}
        }
        Logger.error("⚠ Could not resolve sender from socket " + socket.getRemoteSocketAddress(), null);
        return "unknown";
    }
}
