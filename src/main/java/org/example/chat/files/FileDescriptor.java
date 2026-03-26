package org.example.chat.files;


/**
 * Design choice:
 * Immutable metadata container for a file transfer.
 *
 * Represents:
 * - Transfer ID
 * - Filename
 * - File size
 * - Expected checksum
 *
 * ---------------------------------------------------------
 * Architectural Role
 * ---------------------------------------------------------
 *
 * This is a Value Object.
 *
 * It contains:
 * - No behavior
 * - No state mutation
 *
 * Immutable design ensures:
 * - Thread safety
 * - Predictable transfer validation
 *
 * ---------------------------------------------------------
 * Used for:
 * - Offer negotiation
 * - Upload validation
 * - Integrity checking
 */
public final class FileDescriptor {

    private final String id;
    private final String filename;
    private final long size;
    private final String checksumBase64;

    public FileDescriptor(String id, String filename, long size, String checksumBase64) {
        this.id = id;
        this.filename = filename;
        this.size = size;
        this.checksumBase64 = checksumBase64;
    }

    public String getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }

    public String getChecksumBase64() {
        return checksumBase64;
    }
}
