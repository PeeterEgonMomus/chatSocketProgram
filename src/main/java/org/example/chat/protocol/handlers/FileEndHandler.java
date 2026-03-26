package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.files.ServerFileTransferService;
import org.example.chat.protocol.FrameContext;
import org.example.chat.protocol.FrameType;

/**
 * Handles FILE_END frames.
 *
 * Signals that all chunks have been transmitted.
 *
 * Finalization logic (cleanup, notify recipient, etc.)
 * is delegated to the transfer service.
 */
public class FileEndHandler implements FrameHandler {

    private final ServerFileTransferService transfers;

    public FileEndHandler(ServerFileTransferService transfers) {
        this.transfers = transfers;
    }

    @Override
    public FrameType type() {
        return FrameType.FILE_END;
    }

    @Override
    public void handle(FrameContext ctx) throws Exception {
        transfers.handleFileEnd(ctx.client(), ctx.frame());
    }
}