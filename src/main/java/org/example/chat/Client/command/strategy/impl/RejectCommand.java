package org.example.chat.Client.command.strategy.impl;

import org.example.chat.Client.command.strategy.CommandStrategy;
import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

/**
 * Command strategy for rejecting a file transfer.
 *
 * Usage:
 *   /reject <transferId>
 *
 * Responsibilities:
 * - Send FILE_REJECT frame to server
 *
 * Design:
 * - Does not manage registry state
 * - Server remains source of truth
 */
public final class RejectCommand implements CommandStrategy {

    private final FramedChatConnection connection;

    public RejectCommand(FramedChatConnection connection) {
        this.connection = connection;
    }

    @Override
    public boolean supports(String input) {
        return input.startsWith("/reject ");
    }

    @Override
    public void execute(String input) throws Exception {

        String id = input.substring(8).trim();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeUTF(id);

        connection.send(new Frame(
                FrameType.FILE_REJECT,
                baos.toByteArray()
        ));
    }
}