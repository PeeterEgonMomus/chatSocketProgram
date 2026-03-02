package org.example.chat.Client.protocol;

import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.Client.crypto.ClientCrypto;
import org.example.chat.Client.file.*;
import org.example.chat.Client.handler.ChatFrameHandler;
import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import static org.example.chat.protocol.FrameType.*;

public final class ClientHandlerBootstrap {

    public static void registerAll(
            FrameDispatcher dispatcher,
            IncomingTransferRegistry registry,
            FileTransferService transferService,
            FramedChatConnection connection
    ) {

        dispatcher.register(FrameType.CHAT, new ChatFrameHandler());

        FileTransportHandler transportHandler =
                new FileTransportHandler(registry);

        FileNegotiationHandler negotiationHandler =
                new FileNegotiationHandler(registry, connection);



        dispatcher.register(FILE_OFFER, negotiationHandler);
        dispatcher.register(FILE_START, transportHandler);
        dispatcher.register(FILE_CHUNK, transportHandler);
        dispatcher.register(FILE_END, transportHandler);

        dispatcher.register(FILE_ACCEPT, frame -> {

            DataInputStream in =
                    new DataInputStream(new ByteArrayInputStream(frame.getPayload()));

            String transferId = in.readUTF();

            Logger.debug("FILE_ACCEPT received id=" + transferId);

            // If we are sender
            transferService.onFileAccept(transferId);

            // If we are recipient
            if (registry.getPending(transferId) != null) {
                registry.activate(transferId);
            }
        });


        dispatcher.register(FILE_REJECT, frame -> {

            DataInputStream in =
                    new DataInputStream(new ByteArrayInputStream(frame.getPayload()));

            String transferId = in.readUTF();

            Logger.debug("FILE_REJECT received id=" + transferId);

            // If we are sender
            transferService.onFileReject(transferId);

            // If we are recipient
            if (registry.getPending(transferId) != null) {
                registry.removePending(transferId);
            }
        });

        dispatcher.register(FrameType.SEND_FILE_READY, frame -> {

            DataInputStream in =
                    new DataInputStream(new ByteArrayInputStream(frame.getPayload()));

            String transferId = in.readUTF();

            transferService.onSendFileReady(transferId);
        });


        dispatcher.register(FrameType.ERROR, frame -> {
            String message = new String(frame.getPayload());
            Logger.error("Server error: " + message);
            System.out.println("Server error: " + message);
        });
    }
}
