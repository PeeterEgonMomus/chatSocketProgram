package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.files.ServerFileTransferService;
import org.example.chat.protocol.FrameContext;
import org.example.chat.protocol.FrameType;

public class FileRejectHandler implements FrameHandler {

    private final ServerFileTransferService transfers;

    public FileRejectHandler(ServerFileTransferService transfers) {
        this.transfers = transfers;
    }

    @Override
    public FrameType type() {
        return FrameType.FILE_REJECT;
    }

    @Override
    public void handle(FrameContext ctx) throws Exception {
        transfers.handleFileReject(ctx.client(), ctx.frame());
    }
}