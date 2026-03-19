package org.example.chat.files;

import org.example.chat.ClientHandler;
import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;

public class ServerFileTransferService {

    private final FileTransferManager manager;

    public ServerFileTransferService(FileTransferManager manager) {
        this.manager = manager;
    }

    public void handleSendFileRequest(ClientHandler client, Frame frame) throws Exception {

        DataInputStream in = new DataInputStream(
                new ByteArrayInputStream(
                        client.getServer()
                                .getEncryptionService()
                                .decryptBytesFromClient(client, frame.getType(), frame.getPayload())
                )
        );

        String transferId = in.readUTF();
        String recipientName = in.readUTF();

        if (!client.isAuthenticated()) {
            client.send("You must be authenticated to send files.");
            return;
        }

        var sessionOpt = client.getServer()
                .getSessionManager()
                .getSessionByUsername(recipientName);

        if (sessionOpt.isEmpty()) {
            client.send("User not online.");
            return;
        }

        ClientHandler recipient = sessionOpt.get().getChatHandler();

        if (recipient == client) {
            client.send("You cannot send a file to yourself.");
            return;
        }

        ActiveFileTransfer transfer = manager.createTransfer(transferId, client, recipient);
        transfer.setState(ActiveFileTransfer.State.WAITING_FOR_RECIPIENT);
    }

    public void handleFileOffer(ClientHandler client, Frame frame) throws Exception {

        DataInputStream in = new DataInputStream(
                new ByteArrayInputStream(
                        client.getServer()
                                .getEncryptionService()
                                .decryptBytesFromClient(client, frame.getType(), frame.getPayload())
                )
        );

        String transferId = in.readUTF();
        String filename = in.readUTF();
        long size = in.readLong();
        String checksum = in.readUTF();

        ActiveFileTransfer transfer = manager.getById(transferId);

        if (transfer == null)
            throw new IllegalStateException("Unknown transfer " + transferId);
        if (transfer.getSender() != client)
            throw new IllegalStateException("Sender mismatch " + transferId);
        if (transfer.getState() != ActiveFileTransfer.State.WAITING_FOR_RECIPIENT)
            throw new IllegalStateException("Transfer " + transferId + " not waiting for recipient");

        transfer.registerDescriptor(new FileDescriptor(transferId, filename, size, checksum));

        ClientHandler recipient = (ClientHandler) transfer.getRecipient();

        forwardOffer(client, recipient, transferId, filename, size, checksum);
    }

    private void forwardOffer(ClientHandler sender, ClientHandler recipient,
                              String transferId, String filename, long size, String checksum) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeUTF(transferId);
        out.writeUTF(sender.getUsername());
        out.writeUTF(filename);
        out.writeLong(size);
        out.writeUTF(checksum);

        byte[] encrypted = sender.getServer()
                .getEncryptionService()
                .encryptBytesForClient(recipient, FrameType.FILE_OFFER, baos.toByteArray());

        recipient.sendFrame(new org.example.chat.protocol.Frame(FrameType.FILE_OFFER, encrypted));
    }

    public void handleFileStart(ClientHandler client, Frame frame) throws Exception {

        DataInputStream in = new DataInputStream(
                new ByteArrayInputStream(
                        client.getServer()
                                .getEncryptionService()
                                .decryptBytesFromClient(client, frame.getType(), frame.getPayload())
                )
        );

        String transferId = in.readUTF();

        ActiveFileTransfer transfer = manager.getById(transferId);

        if (transfer == null)
            throw new IllegalStateException("Unknown transfer " + transferId);
        if (transfer.getState() != ActiveFileTransfer.State.UPLOADING)
            throw new IllegalStateException("Transfer not ready for FILE_START");

        transfer.startUploadSession();
    }

    public void handleFileChunk(ClientHandler client, Frame frame) throws Exception {

        DataInputStream in = new DataInputStream(
                new ByteArrayInputStream(
                        client.getServer()
                                .getEncryptionService()
                                .decryptBytesFromClient(client, frame.getType(), frame.getPayload())
                )
        );

        String transferId = in.readUTF();
        int index = in.readInt();
        int len = in.readInt();
        byte[] data = in.readNBytes(len);

        ActiveFileTransfer transfer = manager.getById(transferId);

        if (transfer == null)
            throw new IllegalStateException("Unknown transfer " + transferId);
        if (transfer.getState() != ActiveFileTransfer.State.UPLOADING)
            throw new IllegalStateException("Transfer " + transferId + " not ready for upload");

        transfer.acceptChunk(index, data);
    }

    public void handleFileEnd(ClientHandler client, Frame frame) throws Exception {

        DataInputStream in = new DataInputStream(
                new ByteArrayInputStream(
                        client.getServer()
                                .getEncryptionService()
                                .decryptBytesFromClient(client, frame.getType(), frame.getPayload())
                )
        );

        String transferId = in.readUTF();

        ActiveFileTransfer transfer = manager.getById(transferId);

        if (transfer == null)
            throw new IllegalStateException("Unknown transfer " + transferId);
        if (transfer.getState() != ActiveFileTransfer.State.UPLOADING)
            throw new IllegalStateException("Transfer " + transferId + " not uploading");

        transfer.finishUpload();
        transfer.accept();
    }

    public void handleFileAccept(ClientHandler client, Frame frame) throws Exception {

        String transferId = new String(
                client.getServer()
                        .getEncryptionService()
                        .decryptBytesFromClient(client, frame.getType(), frame.getPayload()),
                StandardCharsets.UTF_8
        );

        ActiveFileTransfer transfer = manager.getById(transferId);

        if (transfer == null || transfer.getRecipient() != client) {
            client.sendEncrypted(FrameType.ERROR,
                    ("Invalid file id: " + transferId).getBytes(StandardCharsets.UTF_8));
            return;
        }

        transfer.setState(ActiveFileTransfer.State.UPLOADING);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeUTF(transferId);

        transfer.getSender().sendEncrypted(FrameType.SEND_FILE_READY, baos.toByteArray());
    }

    public void handleFileReject(ClientHandler client, Frame frame) throws Exception {

        String transferId = new String(
                client.getServer()
                        .getEncryptionService()
                        .decryptBytesFromClient(client, frame.getType(), frame.getPayload()),
                StandardCharsets.UTF_8
        );

        ActiveFileTransfer transfer = manager.getById(transferId);

        if (transfer == null || transfer.getRecipient() != client)
            return;

        transfer.setState(ActiveFileTransfer.State.REJECTED);

        transfer.getSender().sendEncrypted(FrameType.ERROR,
                ("Recipient rejected the file: " + transferId).getBytes(StandardCharsets.UTF_8));

        manager.remove(transferId);
    }
}