package org.example.chat.Client.file;

import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.Client.handler.FrameHandler;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Handles FILE_OFFER frames (incoming file offers).
 *
 * This is the FIRST stage of receiving a file.
 *
 * Responsibilities:
 * - Parse incoming offer metadata
 * - Inform user about incoming file
 * - Store offer as PENDING (not yet active)
 *
 * Important:
 * - No file writing happens here.
 * - Transfer only becomes active after FILE_ACCEPT.
 *
 * Design:
 * Keeps negotiation phase separate from transport phase.
 */
public final class FileNegotiationHandler implements FrameHandler {

    private final IncomingTransferRegistry registry;
    private final FramedChatConnection connection;

    public FileNegotiationHandler(
            IncomingTransferRegistry registry,
            FramedChatConnection connection
    ) {
        this.registry = registry;
        this.connection = connection;
    }

    @Override
    public void handle(Frame frame) throws Exception {

        if (frame.getType() != FrameType.FILE_OFFER) {
            throw new IllegalArgumentException(
                    "FileNegotiationHandler cannot handle " + frame.getType()
            );
        }

        DataInputStream in =
                new DataInputStream(new ByteArrayInputStream(frame.getPayload()));

        String transferId = in.readUTF();
        String sender = in.readUTF();
        String filename = in.readUTF();
        long size = in.readLong();
        String checksum = in.readUTF();

        Logger.info("""
    Incoming file offer!
    From: %s
    File: %s
    Size: %d bytes
    TransferId: %s

    Type /accept %s or /reject %s
    """.formatted(sender, filename, size, transferId, transferId, transferId));

        // ⭐ STORE as pending only
        registry.addPending(
                transferId,
                filename,
                checksum
        );
    }

    /**
     * Auto-accepts an incoming file transfer (for testing / future user prompt)
     *
     * @param transferId The ID of the incoming transfer
     */
    private void autoAccept(String transferId) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeUTF(transferId);

        connection.send(new Frame(
                FrameType.FILE_ACCEPT,
                baos.toByteArray()
        ));

        Logger.debug("Auto-accepted incoming file transfer id=" + transferId);
    }
}
