package org.example.chat.Client.file;

import java.io.File;

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
