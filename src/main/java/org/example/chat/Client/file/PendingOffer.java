package org.example.chat.Client.file;

/**
 * Immutable record representing a file offer
 * that has not yet been accepted or rejected.
 *
 * Exists only during negotiation phase.
 */
public record PendingOffer(
        String transferId,
        String filename,
        String checksum
) {}