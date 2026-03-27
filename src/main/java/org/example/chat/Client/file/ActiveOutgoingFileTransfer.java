package org.example.chat.Client.file;

import java.io.File;


/**
 * Immutable data holder representing a fully prepared outgoing file transfer.
 *
 * This object exists only on the sender side.
 *
 * Responsibilities:
 * - Holds all metadata required for transfer
 * - Holds full file data in memory
 * - Stores checksum for integrity validation
 *
 * Design decisions:
 * - Immutable → thread-safe
 * - Stores entire file in memory (simple but not memory optimal)
 * - Used by FileTransferService during streaming
 *
 * Lifecycle:
 *   prepare() → stored in activeTransfers → streamed → removed
 */
public final class ActiveOutgoingFileTransfer {

    private final String id;
    private final String recipient;
    private final File file;
    private final String checksum;
    private final byte[] data;

    public ActiveOutgoingFileTransfer(
            String id,
            String recipient,
            File file,
            String checksum,
            byte[] data
    ) {
        this.id = id;
        this.recipient = recipient;
        this.file = file;
        this.checksum = checksum;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public String getRecipient() {
        return recipient;
    }

    public File getFile() {
        return file;
    }

    public String getChecksum() {
        return checksum;
    }

    public byte[] getData() {
        return data;
    }

    public long getSize() {
        return data.length;
    }
}
