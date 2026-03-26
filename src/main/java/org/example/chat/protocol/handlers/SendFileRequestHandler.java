package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.files.ServerFileTransferService;
import org.example.chat.protocol.FrameContext;
import org.example.chat.protocol.FrameType;

/**
 * Handles SEND_FILE_REQUEST frames.
 *
 * This initiates a file transfer workflow.
 *
 * Flow:
 * Sender → SEND_FILE_REQUEST
 * Server  → FILE_OFFER to recipient
 *
 * Again:
 * This class only delegates to the transfer domain service.
 */
public class SendFileRequestHandler implements FrameHandler {

    private final ServerFileTransferService transfers;

    public SendFileRequestHandler(ServerFileTransferService transfers) {
        this.transfers = transfers;
    }

    @Override
    public FrameType type() {
        return FrameType.SEND_FILE_REQUEST;
    }

    @Override
    public void handle(FrameContext ctx) throws Exception {
        transfers.handleSendFileRequest(ctx.client(), ctx.frame());
    }
}