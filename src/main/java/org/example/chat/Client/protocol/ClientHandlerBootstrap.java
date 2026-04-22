package org.example.chat.Client.protocol;

import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.Client.file.*;
import org.example.chat.Client.handler.ChatFrameHandler;
import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;
import org.example.chat.Client.handler.PingFrameHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import static org.example.chat.protocol.FrameType.*;

/**
 * ClientHandlerBootstrap registers all standard frame handlers
 * for chat messages, file transfers, and error frames.
 *
 * Responsibilities:
 * - Register frame handlers with FrameDispatcher
 * - Handle both sender and recipient file transfer flows
 * - Parse incoming payloads for specific frame types
 *
 * Architecture Role:
 * - Bootstraps client protocol layer
 * - Separates registration logic from runtime
 */
public final class ClientHandlerBootstrap {

    public static void registerAll(
            FrameDispatcher dispatcher,
            IncomingTransferRegistry registry,
            FileTransferService transferService,
            FramedChatConnection connection
    ) {
        // Chat message frames
        dispatcher.register(FrameType.CHAT, new ChatFrameHandler());

        // File transfer frames
        FileTransportHandler transportHandler = new FileTransportHandler(registry);
        FileNegotiationHandler negotiationHandler = new FileNegotiationHandler(registry, connection);


        dispatcher.register(FILE_OFFER, negotiationHandler);
        dispatcher.register(FILE_START, transportHandler);
        dispatcher.register(FILE_CHUNK, transportHandler);
        dispatcher.register(FILE_END, transportHandler);


        // FILE_ACCEPT
        dispatcher.register(FILE_ACCEPT, frame -> {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(frame.getPayload()));
            String transferId = in.readUTF();
            Logger.debug("FILE_ACCEPT received id=" + transferId);

            transferService.onFileAccept(transferId);
            if (registry.getPending(transferId) != null) {
                registry.activate(transferId);
            }
        });

        // FILE_REJECT
        dispatcher.register(FILE_REJECT, frame -> {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(frame.getPayload()));
            String transferId = in.readUTF();
            Logger.debug("FILE_REJECT received id=" + transferId);

            transferService.onFileReject(transferId);
            if (registry.getPending(transferId) != null) {
                registry.removePending(transferId);
            }
        });

        // SEND_FILE_READY
        dispatcher.register(FrameType.SEND_FILE_READY, frame -> {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(frame.getPayload()));
            String transferId = in.readUTF();
            transferService.onSendFileReady(transferId);
        });

        dispatcher.register(FrameType.PING,
                new PingFrameHandler(connection));

        // ERROR frame
        dispatcher.register(FrameType.ERROR, frame -> {
            String message = new String(frame.getPayload());
            Logger.error("Server error: " + message);
            System.out.println("Server error: " + message);
        });
    }
}