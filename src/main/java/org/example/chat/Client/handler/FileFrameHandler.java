package org.example.chat.Client.handler;

import org.example.chat.Client.crypto.ClientCrypto;
import org.example.chat.Client.file.IncomingFileState;
import org.example.chat.protocol.Frame;
import org.example.chat.util.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.Base64;

public final class FileFrameHandler implements FrameHandler {

    private final IncomingFileState state;
    private final ClientCrypto crypto;

    public FileFrameHandler(IncomingFileState state, ClientCrypto crypto) {
        this.state = state;
        this.crypto = crypto;
    }

    @Override
    public void handle(Frame frame) throws Exception {
        switch (frame.getType()) {

            case FILE_META -> handleMeta(frame);
            case FILE_CHUNK -> handleChunk(frame);
            case FILE_END -> handleEnd(frame);

            default -> throw new IllegalArgumentException(
                    "FileFrameHandler cannot handle frame type " + frame.getType()
            );
        }
    }

    private void handleMeta(Frame frame) throws Exception {
        String meta = new String(frame.getPayload());
        String[] parts = meta.split("\\|");

        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid FILE_META frame: " + meta);
        }

        String sender = parts[0];
        String filename = parts[1];
        String checksum = parts[3];

        Logger.info("Receiving file '" + filename + "' from " + sender);

        state.start(filename, checksum);
    }

    private void handleChunk(Frame frame) throws Exception {
        if (!state.isActive()) {
            throw new IllegalStateException("Received FILE_CHUNK without FILE_META");
        }

        byte[] payload = frame.getPayload();
        state.write(payload);

        Logger.debug("Received file chunk (" + payload.length + " bytes)");
    }

    private void handleEnd(Frame frame) throws Exception {
        if (!state.isActive()) {
            throw new IllegalStateException("Received FILE_END without FILE_META");
        }

        byte[] digestBytes = state.finish();
        String receivedChecksum =
                Base64.getEncoder().encodeToString(digestBytes);

        if (!state.verifyChecksum(receivedChecksum)) {
            Logger.error("Checksum mismatch for file " + state.getFilename());
            state.getFile().delete();
            System.out.println("File '" + state.getFilename() + "' failed checksum verification.");
        } else {
            Logger.info("File '" + state.getFilename() + "' received successfully.");
            System.out.println("File '" + state.getFilename() + "' received successfully.");
        }

        state.reset();
    }
}
