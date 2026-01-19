package org.example.chat.files;

import org.example.chat.util.Logger;

import java.io.*;
import java.security.MessageDigest;
import java.util.Base64;

public final class FileTransferSession {

    private static final int MAX_FILE_SIZE = 100_000_000;

    private FileOutputStream out;
    private String filename;
    private String recipient;
    private long expectedSize;
    private long bytesReceived;
    private MessageDigest digest;
    private String expectedChecksum;
    private int expectedChunkIndex;

    public void start(String recipient, String filename, long size, String checksum) throws Exception {
        if (size <= 0 || size > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Invalid file size: " + size);
        }

        this.recipient = recipient;
        this.filename = filename;
        this.expectedSize = size;
        this.expectedChecksum = checksum;
        this.out = new FileOutputStream("received_" + filename);
        this.digest = MessageDigest.getInstance("SHA-256");
        this.bytesReceived = 0;
        this.expectedChunkIndex = 0;

        Logger.info("Started receiving file '" + filename + "' (" + size + " bytes)");
    }

    public void acceptChunk(int index, byte[] data) throws IOException {
        if (index != expectedChunkIndex) {
            throw new IllegalStateException(
                    "Chunk order violation: expected " + expectedChunkIndex + " got " + index
            );
        }

        out.write(data);
        digest.update(data);
        bytesReceived += data.length;
        expectedChunkIndex++;
    }

    public boolean isComplete() {
        return bytesReceived == expectedSize;
    }

    public void finish() throws Exception {
        out.close();

        if (bytesReceived != expectedSize) {
            throw new IllegalStateException("File size mismatch");
        }

        String checksum = Base64.getEncoder().encodeToString(digest.digest());
        if (!checksum.equals(expectedChecksum)) {
            throw new IllegalStateException("Checksum mismatch");
        }

        Logger.info("File '" + filename + "' received successfully");
    }

    public File getFile() {
        return new File("received_" + filename);
    }

    public String getRecipient() { return recipient; }
    public String getFilename() { return filename; }
    public long getSize() { return bytesReceived; }

    public void abort() {
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        if (filename != null) new File("received_" + filename).delete();
    }
}
