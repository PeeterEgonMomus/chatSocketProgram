package org.example.chat.Client.file;

import org.example.chat.Client.handler.FrameHandler;
import org.example.chat.protocol.Frame;
import org.example.chat.util.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public final class FileTransportHandler implements FrameHandler {

    private final IncomingTransferRegistry registry;

    public FileTransportHandler(IncomingTransferRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void handle(Frame frame) throws Exception {

        switch (frame.getType()) {

            case FILE_START -> handleStart(frame);

            case FILE_CHUNK -> handleChunk(frame);

            case FILE_END -> handleEnd(frame);

            default -> throw new IllegalArgumentException(
                    "Transport handler cannot handle " + frame.getType()
            );
        }
    }

    private void handleStart(Frame frame) throws Exception {

        DataInputStream in =
                new DataInputStream(new ByteArrayInputStream(frame.getPayload()));

        String transferId = in.readUTF();

        IncomingFileState state = registry.get(transferId);

        if (state == null) {
            throw new IllegalStateException(
                    "FILE_START for unknown transfer: " + transferId
            );
        }

        Logger.debug("File transfer started id=" + transferId);
    }

    private void handleChunk(Frame frame) throws Exception {

        DataInputStream in =
                new DataInputStream(new ByteArrayInputStream(frame.getPayload()));

        String transferId = in.readUTF();
        int index = in.readInt();
        int len = in.readInt();

        IncomingFileState state = registry.get(transferId);

        if (state == null) {
            throw new IllegalStateException(
                    "Chunk for unknown transfer: " + transferId
            );
        }

        byte[] data = in.readNBytes(len);

        // ⭐ NON-BLOCKING: enqueue only
        state.enqueue(data);

        Logger.debug(
                "Chunk #" + index +
                        " queued (" + len + " bytes) for transfer " + transferId
        );
    }

    private void handleEnd(Frame frame) throws Exception {

        DataInputStream in =
                new DataInputStream(new ByteArrayInputStream(frame.getPayload()));

        String transferId = in.readUTF();

        IncomingFileState state = registry.get(transferId);

        if (state == null) {
            throw new IllegalStateException(
                    "End for unknown transfer: " + transferId
            );
        }

        // ⭐ SIGNAL completion — writer thread finishes file & checksum
        state.signalEnd();

        registry.remove(transferId);

        Logger.debug("File transfer completed id=" + transferId);
    }
}
