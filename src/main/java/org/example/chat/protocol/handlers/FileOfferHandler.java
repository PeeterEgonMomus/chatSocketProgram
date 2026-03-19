package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.files.ServerFileTransferService;
import org.example.chat.protocol.FrameContext;
import org.example.chat.protocol.FrameType;

public class FileOfferHandler implements FrameHandler {

    private final ServerFileTransferService transfers;

    public FileOfferHandler(ServerFileTransferService transfers) {
        this.transfers = transfers;
    }

    @Override
    public FrameType type() {
        return FrameType.FILE_OFFER;
    }

    @Override
    public void handle(FrameContext ctx) throws Exception {
        transfers.handleFileOffer(ctx.client(), ctx.frame());
    }
}