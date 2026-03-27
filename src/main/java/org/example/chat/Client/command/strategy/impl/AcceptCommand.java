package org.example.chat.Client.command.strategy.impl;

import org.example.chat.Client.command.strategy.CommandStrategy;
import org.example.chat.Client.file.IncomingTransferRegistry;
import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.protocol.*;

import java.io.*;

/**
 * Command strategy for accepting a pending file transfer.
 *
 * Usage:
 *   /accept <transferId>
 *
 * Responsibilities:
 * - Activate pending transfer locally
 * - Notify server of acceptance via FILE_ACCEPT frame
 *
 * Flow:
 *   1. User accepts
 *   2. Registry activates transfer
 *   3. Client notifies server
 *
 * Note:
 * - Relies on IncomingTransferRegistry for state management
 * - Constructs protocol frame manually
 */
public final class AcceptCommand implements CommandStrategy {

    private final IncomingTransferRegistry registry;
    private final FramedChatConnection connection;

    public AcceptCommand(
            IncomingTransferRegistry registry,
            FramedChatConnection connection
    ) {
        this.registry = registry;
        this.connection = connection;
    }

    @Override
    public boolean supports(String input) {
        return input.startsWith("/accept ");
    }

    @Override
    public void execute(String input) throws Exception {

        String id = input.substring(8).trim();

        registry.activate(id);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeUTF(id);

        connection.send(new Frame(
                FrameType.FILE_ACCEPT,
                baos.toByteArray()
        ));
    }
}