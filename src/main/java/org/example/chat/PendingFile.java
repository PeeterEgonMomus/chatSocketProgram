package org.example.chat;

import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameEncoder;
import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Base64;

public class PendingFile {

    private static final int CHUNK_SIZE = 4096;

    private final ClientHandler sender;
    private final ClientHandler recipient;
    private final File file;
    private final long size;
    private final String checksumBase64;

    public PendingFile(ClientHandler sender,
                       ClientHandler recipient,
                       File file,
                       long size,
                       String checksumBase64) {
        this.sender = sender;
        this.recipient = recipient;
        this.file = file;
        this.size = size;
        this.checksumBase64 = checksumBase64;
    }

    public ClientHandler getSender() {
        return sender;
    }

    public ClientHandler getRecipient() {
        return recipient;
    }

    public String getFilename() {
        return file.getName().replaceFirst("^received_", "");
    }

    public void sendToRecipient() throws IOException {
        Logger.info("Sending file '" + file.getName() + "' to '" + recipient + "'");

        var out = recipient.getSocket().getOutputStream();

        // ================= FILE_META: filename|size|checksum =================
        String meta = getFilename() + "|" + size + "|" + checksumBase64;
        FrameEncoder.write(
                new Frame(FrameType.FILE_META, meta.getBytes()),
                out
        );

        // ================= FILE_CHUNK(s) =================
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                byte[] chunk = new byte[read];
                System.arraycopy(buffer, 0, chunk, 0, read);

                FrameEncoder.write(
                        new Frame(FrameType.FILE_CHUNK, chunk),
                        out
                );
            }
        }

        // ================= FILE_END =================
        FrameEncoder.write(
                new Frame(FrameType.FILE_END, getFilename().getBytes()),
                out
        );

        Logger.info("File delivery completed: " + file.getName());
    }

    public void cleanup() {
        if (file.exists()) {
            if (file.delete()) {
                Logger.debug("Pending file deleted: " + file.getName());
            } else {
                Logger.error("Failed to delete pending file: " + file.getName());
            }
        }
    }

    public String getChecksumBase64() {
        return checksumBase64;
    }
}
