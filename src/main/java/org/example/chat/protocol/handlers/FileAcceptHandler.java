package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.files.ServerFileTransferService;
import org.example.chat.protocol.FrameContext;
import org.example.chat.protocol.FrameType;

public class FileAcceptHandler implements FrameHandler {

    private final ServerFileTransferService transfers;

    public FileAcceptHandler(ServerFileTransferService transfers) {
        this.transfers = transfers;
    }

    @Override
    public FrameType type() {
        return FrameType.FILE_ACCEPT;
    }

    @Override
    public void handle(FrameContext ctx) throws Exception {
        transfers.handleFileAccept(ctx.client(), ctx.frame());
    }
}