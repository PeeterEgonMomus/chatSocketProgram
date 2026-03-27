package org.example.chat.Client.file;

import org.example.chat.Client.handler.FrameHandler;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

/**
 * Handles FILE_ACCEPT and FILE_REJECT frames.
 *
 * This handler is part of the negotiation lifecycle.
 *
 * Responsibilities:
 * - React to recipient decision
 * - Activate pending transfers on accept
 * - Remove pending transfers on reject
 *
 * Important:
 * This handler does NOT perform any file IO.
 * It only coordinates registry state.
 */
public final class FileDecisionHandler implements FrameHandler {

    private final IncomingTransferRegistry registry;

    public FileDecisionHandler(IncomingTransferRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void handle(Frame frame) throws Exception {

        DataInputStream in =
                new DataInputStream(new ByteArrayInputStream(frame.getPayload()));

        String transferId = in.readUTF();

        switch (frame.getType()) {

            case FILE_ACCEPT -> {

                Logger.debug("Transfer accepted id=" + transferId);

                registry.activatePendingTransfer(transferId);
            }

            case FILE_REJECT -> {

                Logger.debug("Transfer rejected id=" + transferId);

                registry.removePending(transferId);
            }

            default -> throw new IllegalArgumentException(
                    "Decision handler cannot handle " + frame.getType()
            );
        }
    }
}