package org.example.chat;

import org.example.chat.files.*;
import org.example.chat.files.FileDescriptor;
import org.example.chat.protocol.*;
import org.example.chat.util.Logger;

import java.io.*;

public final class PendingFile {

    private static final int CHUNK_SIZE = 4096;

    private final String id;
    private final FileTransferPeer sender;
    private final FileTransferPeer recipient;
    private final FileDescriptor descriptor;
    private final File file;

    public PendingFile(
            String id,
            FileTransferPeer sender,
            FileTransferPeer recipient,
            FileDescriptor descriptor,
            File file
    )
    {
        this.id = id;
        this.sender = sender;
        this.recipient = recipient;
        this.descriptor = descriptor;
        this.file = file;
    }

    public void startDelivery() throws Exception {

        Logger.info("Sending FILE_START to " + recipient.getUsername());

        sendFileStart();
        streamFileChunks();
        sendFileEnd();
    }


    /* ================= PRIVATE ================= */

    private void sendFileStart() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(baos);

        data.writeUTF(id);   // ONLY ID

        recipient.sendEncrypted(
                FrameType.FILE_START,
                baos.toByteArray()
        );
    }



    private void streamFileChunks() throws Exception {

        try (FileInputStream in = new FileInputStream(file)) {

            byte[] buffer = new byte[CHUNK_SIZE];
            int index = 0;
            int read;

            while ((read = in.read(buffer)) != -1) {

                byte[] payload =
                        ChunkCodec.encode(id, index++, buffer, read);

                recipient.sendEncrypted(
                        FrameType.FILE_CHUNK,
                        payload
                );
            }
        }
    }


    private void sendFileEnd() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(baos);

        data.writeUTF(id);   // MATCHES readUTF()

        recipient.sendEncrypted(
                FrameType.FILE_END,
                baos.toByteArray()
        );
    }


    public void cleanup() {
        if (file.exists()) file.delete();
    }
}
