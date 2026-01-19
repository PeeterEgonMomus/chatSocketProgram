package org.example.chat.Client.file;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;

public final class IncomingFileState {

    private File file;
    private FileOutputStream out;
    private MessageDigest digest;
    private String expectedChecksum;
    private String filename;

    public void start(String filename, String expectedChecksum) throws Exception {
        this.filename = filename;
        this.expectedChecksum = expectedChecksum;

        this.file = new File("received_" + filename);
        this.out = new FileOutputStream(file);
        this.digest = MessageDigest.getInstance("SHA-256");
    }

    public void write(byte[] data) throws Exception {
        out.write(data);
        digest.update(data);
    }

    public boolean verifyChecksum(String receivedChecksum) {
        return receivedChecksum.equals(expectedChecksum);
    }

    public byte[] finish() throws Exception {
        out.close();
        return digest.digest();
    }

    public File getFile() {
        return file;
    }

    public String getFilename() {
        return filename;
    }

    public void reset() {
        file = null;
        out = null;
        digest = null;
        expectedChecksum = null;
        filename = null;
    }

    public boolean isActive() {
        return out != null;
    }
}
