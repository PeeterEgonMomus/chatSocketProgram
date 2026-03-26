package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.files.ServerFileTransferService;
import org.example.chat.protocol.FrameContext;
import org.example.chat.protocol.FrameType;

/**
 * Handles FILE_START frames.
 *
 * Indicates that actual file data transmission is about to begin.
 *
 * The handler does not track transfer state —
 * that responsibility belongs to ServerFileTransferService.
 */
public class FileStartHandler implements FrameHandler {

    private final ServerFileTransferService transfers;

    public FileStartHandler(ServerFileTransferService transfers) {
        this.transfers = transfers;
    }

    @Override
    public FrameType type() {
        return FrameType.FILE_START;
    }

    @Override
    public void handle(FrameContext ctx) throws Exception {
        transfers.handleFileStart(ctx.client(), ctx.frame());
    }
}