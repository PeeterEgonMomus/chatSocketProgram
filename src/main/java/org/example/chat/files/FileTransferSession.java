package org.example.chat.files;

import org.example.chat.util.Logger;

import java.io.*;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;

public final class FileTransferSession {

    private static final int MAX_FILE_SIZE = 100_000_000;

    private FileOutputStream out;
    private String filename;
    private String sender;

    private long expectedSize;
    private long bytesReceived;

    private MessageDigest digest;
    private String expectedChecksum;
    private String actualChecksumBase64;

    private int expectedChunkIndex;

    public void start(String sender, String filename, long size, String checksum) throws Exception {
        if (size <= 0 || size > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Invalid file size: " + size);
        }

        filename = Path.of(filename).getFileName().toString();


        this.sender = sender;
        this.filename = filename;
        this.expectedSize = size;
        this.expectedChecksum = checksum;

        this.out = new FileOutputStream("received_" + filename);
        this.digest = MessageDigest.getInstance("SHA-256");

        this.bytesReceived = 0;
        this.expectedChunkIndex = 0;
        this.actualChecksumBase64 = null;

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

    public void finish() throws Exception {
        out.close();

        if (bytesReceived != expectedSize) {
            throw new IllegalStateException(
                    "File size mismatch: expected " + expectedSize + " got " + bytesReceived
            );
        }

        actualChecksumBase64 =
                Base64.getEncoder().encodeToString(digest.digest());

        if (!actualChecksumBase64.equals(expectedChecksum)) {
            throw new IllegalStateException(
                    "Checksum mismatch: expected " + expectedChecksum +
                            " got " + actualChecksumBase64
            );
        }

        Logger.info("File '" + filename + "' received successfully");
    }

    /* ================= ACCESSORS ================= */

    public File getFile() {
        return new File("received_" + filename);
    }

    public String getFilename() {
        return filename;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

    public String getChecksumBase64() {
        if (actualChecksumBase64 == null) {
            throw new IllegalStateException("Checksum not available before finish()");
        }
        return actualChecksumBase64;
    }

    /* ================= ABORT ================= */

    public void abort() {
        try {
            if (out != null) out.close();
        } catch (IOException ignored) {}

        if (filename != null) {
            new File("received_" + filename).delete();
        }
    }
}
