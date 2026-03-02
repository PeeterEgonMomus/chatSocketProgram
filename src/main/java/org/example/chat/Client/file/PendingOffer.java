package org.example.chat.Client.file;

public record PendingOffer(
        String transferId,
        String filename,
        String checksum
) {}