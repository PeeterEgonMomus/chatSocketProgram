package org.example.chat.Client.file;

import org.example.chat.util.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents the active state of a single incoming file transfer.
 *
 * Responsibilities:
 * - Manage file output stream
 * - Compute SHA-256 checksum
 * - Write data asynchronously via writer thread
 * - Validate integrity upon completion
 *
 * Concurrency Model:
 * - Uses BlockingQueue to decouple network thread from disk IO
 * - Network thread enqueues data (non-blocking)
 * - Dedicated writer thread consumes queue
 *
 * Safety:
 * - Uses POISON pill pattern to signal completion
 * - Deletes file on checksum mismatch
 *
 * This design prevents:
 * - Blocking network thread
 * - Backpressure delays
 * - Corruption from concurrent writes
 */
public final class IncomingFileState {

    private File file;
    private FileOutputStream out;
    private MessageDigest digest;
    private String expectedChecksum;
    private String filename;
    private String transferId;

    // ⭐ NEW
    private final BlockingQueue<byte[]> queue =
            new LinkedBlockingQueue<>();

    private static final byte[] POISON = new byte[0];

    public void start(
            String transferId,
            String filename,
            String expectedChecksum
    ) throws Exception {

        this.transferId = transferId;
        this.filename = filename;
        this.expectedChecksum = expectedChecksum;

        this.file = new File("received_" + filename);
        this.out = new FileOutputStream(file);
        this.digest = MessageDigest.getInstance("SHA-256");

        startWriterThread(); // ⭐ CRITICAL
    }

    // ⭐ called by handler (FAST)
    public void enqueue(byte[] data) {
        queue.add(data);
    }

    // ⭐ called when FILE_END arrives
    public void signalEnd() {
        queue.add(POISON);
    }

    private void startWriterThread() {

        Thread writer = new Thread(() -> {

            try {

                while (true) {

                    byte[] data = queue.take();

                    if (data == POISON)
                        break;

                    out.write(data);
                    digest.update(data);
                }

                out.close();

                String receivedChecksum =
                        Base64.getEncoder().encodeToString(digest.digest());

                if (!receivedChecksum.equals(expectedChecksum)) {

                    Logger.error("Checksum mismatch for " + filename);
                    file.delete();

                } else {

                    Logger.info("File received successfully: " + filename);
                }

            } catch (Exception e) {

                Logger.error("File writer crashed for " + filename, e);
            }

        }, "file-writer-" + transferId);

        writer.setDaemon(true);
        writer.start();
    }

    public File getFile() {
        return file;
    }

    public String getFilename() {
        return filename;
    }
}

