package org.example.chat.Client.protocol;

import org.example.chat.Client.handler.ChatFrameHandler;
import org.example.chat.Client.crypto.ClientCrypto;
import org.example.chat.Client.handler.FileFrameHandler;
import org.example.chat.Client.file.IncomingFileState;
import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;

public final class ClientFrameRegistry {

    public static void registerAll(FrameDispatcher dispatcher, ClientCrypto crypto, IncomingFileState fileState) {
        // Chat messages
        dispatcher.register(FrameType.CHAT, new ChatFrameHandler());

        // File-related frames
        FileFrameHandler fileHandler = new FileFrameHandler(fileState, crypto);
        dispatcher.register(FrameType.FILE_META, fileHandler);
        dispatcher.register(FrameType.FILE_CHUNK, fileHandler);
        dispatcher.register(FrameType.FILE_END, fileHandler);

        // File transfer responses
        dispatcher.register(FrameType.FILE_ACCEPT, frame -> {
            String filename = new String(frame.getPayload());
            Logger.info("Server accepted file: " + filename);
            System.out.println("Server accepted file: " + filename);
        });

        dispatcher.register(FrameType.FILE_REJECT, frame -> {
            String filename = new String(frame.getPayload());
            Logger.error("Server rejected file: " + filename);
            System.out.println("Server rejected file: " + filename);
        });

        // General errors
        dispatcher.register(FrameType.ERROR, frame -> {
            String message = new String(frame.getPayload());
            Logger.error("Server error: " + message);
            System.out.println("Server error: " + message);
        });
    }
}
