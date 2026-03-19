package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.files.ServerFileTransferService;
import org.example.chat.protocol.FrameContext;
import org.example.chat.protocol.FrameType;

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