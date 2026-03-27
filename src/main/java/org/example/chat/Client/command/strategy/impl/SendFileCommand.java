package org.example.chat.Client.command.strategy.impl;

import org.example.chat.Client.command.strategy.CommandStrategy;
import org.example.chat.Client.file.FileTransferService;

/**
 * Command strategy for sending a file to another user.
 *
 * Usage:
 *   /sendfile <recipient> <path>
 *
 * Responsibilities:
 * - Parse user input
 * - Delegate transfer preparation to FileTransferService
 *
 * Design:
 * - Command handles parsing only
 * - Business logic resides in FileTransferService
 * - Maintains Single Responsibility Principle
 */
public final class SendFileCommand implements CommandStrategy {

    private final FileTransferService service;

    public SendFileCommand(FileTransferService service) {
        this.service = service;
    }

    @Override
    public boolean supports(String input) {
        return input.startsWith("/sendfile ");
    }

    @Override
    public void execute(String input) throws Exception {
        String[] parts = input.split(" ", 3);

        if (parts.length < 3) {
            return;
        }

        service.prepareAndRequest(parts[1], parts[2]);
    }
}