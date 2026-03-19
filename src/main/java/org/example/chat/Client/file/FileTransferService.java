package org.example.chat.Client.file;

import org.example.chat.Client.connection.FramedChatConnection;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;

import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FileTransferService {

    private final FramedChatConnection connection;

    private final Map<String, ActiveOutgoingFileTransfer> activeTransfers =
            new ConcurrentHashMap<>();

    public FileTransferService(FramedChatConnection connection) {
        this.connection = connection;
    }

    /*
        STEP 1: Sender initiates transfer
        → SEND_FILE_REQUEST
        → FILE_OFFER
     */
    public void prepareAndRequest(String recipient, String path) throws Exception {

        ActiveOutgoingFileTransfer transfer = prepare(recipient, path);

        activeTransfers.put(transfer.getId(), transfer);

        sendFileRequest(transfer);
        sendOffer(transfer); // 🔥 immediately send offer

        Logger.debug("SEND_FILE_REQUEST + FILE_OFFER sent id=" + transfer.getId());
    }

    /*
        STEP 2: Server signals upload may begin
        → stream data
     */
    public void onSendFileReady(String transferId) throws Exception {

        ActiveOutgoingFileTransfer transfer =
                activeTransfers.get(transferId);

        if (transfer == null) {
            Logger.error("READY for unknown transfer: " + transferId);
            return;
        }

        Logger.debug("Server approved transfer. Starting stream id=" + transferId);

        sendData(transfer);

        activeTransfers.remove(transferId);
    }

    /*
        Recipient rejected
     */
    public void onFileReject(String transferId) {
        activeTransfers.remove(transferId);
        Logger.debug("Transfer rejected id=" + transferId);
    }

    /*
        We no longer react to FILE_ACCEPT directly.
        Server controls upload start via SEND_FILE_READY.
     */
    public void onFileAccept(String transferId) {
        Logger.debug("Ignoring FILE_ACCEPT (handled by server) id=" + transferId);
    }

    // ================= INTERNAL =================

    private ActiveOutgoingFileTransfer prepare(String recipient, String path)
            throws Exception {

        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Invalid file: " + path);
        }

        byte[] data = Files.readAllBytes(file.toPath());

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String checksum =
                Base64.getEncoder().encodeToString(digest.digest(data));

        return new ActiveOutgoingFileTransfer(
                UUID.randomUUID().toString(),
                recipient,
                file,
                checksum,
                data
        );
    }

    private void sendFileRequest(ActiveOutgoingFileTransfer t) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeUTF(t.getId());
        out.writeUTF(t.getRecipient());

        connection.send(new Frame(
                FrameType.SEND_FILE_REQUEST,
                baos.toByteArray()
        ));
    }

    private void sendOffer(ActiveOutgoingFileTransfer t) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeUTF(t.getId());
        out.writeUTF(t.getFile().getName());
        out.writeLong(t.getSize());
        out.writeUTF(t.getChecksum());

        connection.send(new Frame(
                FrameType.FILE_OFFER,
                baos.toByteArray()
        ));

        Logger.debug("FILE_OFFER sent id=" + t.getId());
    }

    private void sendData(ActiveOutgoingFileTransfer t) throws Exception {

        // FILE_START
        ByteArrayOutputStream start = new ByteArrayOutputStream();
        DataOutputStream startOut = new DataOutputStream(start);
        startOut.writeUTF(t.getId());

        connection.send(new Frame(
                FrameType.FILE_START,
                start.toByteArray()
        ));

        final int CHUNK_SIZE = 4096;
        byte[] data = t.getData();

        int offset = 0;
        int index = 0;

        while (offset < data.length) {

            int len = Math.min(CHUNK_SIZE, data.length - offset);

            ByteArrayOutputStream chunk = new ByteArrayOutputStream();
            DataOutputStream chunkOut = new DataOutputStream(chunk);

            chunkOut.writeUTF(t.getId());
            chunkOut.writeInt(index++);
            chunkOut.writeInt(len);
            chunkOut.write(data, offset, len);

            connection.send(new Frame(
                    FrameType.FILE_CHUNK,
                    chunk.toByteArray()
            ));

            offset += len;
        }

        // FILE_END
        ByteArrayOutputStream end = new ByteArrayOutputStream();
        new DataOutputStream(end).writeUTF(t.getId());

        connection.send(new Frame(
                FrameType.FILE_END,
                end.toByteArray()
        ));

        Logger.debug("Completed transfer id=" + t.getId());
    }
}
