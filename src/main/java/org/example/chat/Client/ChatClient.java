package org.example.chat.Client;

import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.*;


public class ChatClient {

    private final String host;
    private final int port;
    private final ClientCrypto crypto;


    private File currentIncomingFile;
    private FileOutputStream currentIncomingFileOut;
    private MessageDigest currentIncomingDigest;
    private String expectedIncomingChecksum;
    private String incomingFileName;


    private FramedChatConnection framedConnection;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "chat-client-reader");
                t.setDaemon(true);
                return t;
            });

    public ChatClient(String host, int port, ClientCrypto crypto) {
        this.host = host;
        this.port = port;
        this.crypto = crypto;
    }

    public void start() {
        try (Socket socket = new Socket(host, port);
             ChatConnection textConn = new SocketChatConnection(socket);
             BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in))) {

            Logger.info("Connected to server");

            // ================= HANDSHAKE (TEXT ONLY) =================
            HandshakeManager handshake = new HandshakeManager(crypto, textConn);
            if (!handshake.doClientHandshake()) {
                return;
            }

            // ================= SWITCH TO FRAMES =================
            framedConnection = new FramedChatConnection(socket);

            executor.submit(() -> readLoop());

            // ================= SEND LOOP =================
            String input;
            while ((input = userIn.readLine()) != null) {
                if (input.equalsIgnoreCase("/quit")) break;

                // ================= FILE ACCEPT =================
                if (input.startsWith("/accept ")) {
                    String filename = input.substring(8).trim();
                    sendFileAccept(filename);
                    continue;
                }

                // ================= FILE REJECT =================
                if (input.startsWith("/reject ")) {
                    String filename = input.substring(8).trim();
                    sendFileReject(filename);
                    continue;
                }

                // ================= SEND FILE =================
                if (input.startsWith("/sendfile ")) {
                    String[] parts = input.split(" ", 3);
                    if (parts.length < 3) {
                        System.out.println("Usage: /sendfile <user> <path>");
                        continue;
                    }

                    sendFile(parts[1], Path.of(parts[2]));
                    continue;
                }

                // ================= CHAT =================
                byte[] encrypted =
                        crypto.encryptForServer(input).getBytes(StandardCharsets.UTF_8);

                framedConnection.send(new Frame(FrameType.CHAT, encrypted));
            }


        } catch (Exception e) {
            Logger.error("Client error", e);
        } finally {
            shutdown();
        }
    }

    private void readLoop() {
        try {
            Frame frame;
            while ((frame = framedConnection.receive()) != null) {
                handleFrame(frame);
            }
        } catch (Exception e) {
            Logger.error("Connection closed", e);
        }
    }

    private void handleFrame(Frame frame) {
        try {
            switch (frame.getType()) {

                /* ================= CHAT ================= */
                case CHAT -> {
                    String encrypted = new String(frame.getPayload(), StandardCharsets.UTF_8);
                    String decrypted = crypto.decryptFromServer(encrypted);
                    System.out.println(decrypted);
                }

                /* ================= FILE META ================= */
                case FILE_META -> {
                    String meta = new String(frame.getPayload(), StandardCharsets.UTF_8);
                    // Support both formats:
                    // 1. sender|filename|size|checksum  (initial notification)
                    // 2. filename|size|checksum         (actual transfer after /accept)
                    String[] parts = meta.split("\\|");
                    if (parts.length == 4) {
                        incomingFileName = parts[1];
                        expectedIncomingChecksum = parts[3];
                        Logger.info("Receiving file '" + incomingFileName + "' from " + parts[0]);
                    } else if (parts.length == 3) {
                        incomingFileName = parts[0];
                        expectedIncomingChecksum = parts[2];
                        Logger.info("Receiving file '" + incomingFileName + "'");
                    } else {
                        Logger.error("Invalid FILE_META received: " + meta);
                        return;
                    }

                    // Initialize file stream and digest for actual transfer
                    currentIncomingFile = new File("received_" + incomingFileName);
                    currentIncomingFileOut = new FileOutputStream(currentIncomingFile);
                    currentIncomingDigest = MessageDigest.getInstance("SHA-256");
                }

                /* ================= FILE CHUNK ================= */
                case FILE_CHUNK -> {
                    if (currentIncomingFileOut == null) {
                        Logger.error("Received FILE_CHUNK without FILE_META, ignoring.");
                        return;
                    }

                    currentIncomingFileOut.write(frame.getPayload());
                    currentIncomingDigest.update(frame.getPayload());

                    Logger.debug("Received file chunk (" + frame.getPayload().length + " bytes)");
                }

                /* ================= FILE END ================= */
                case FILE_END -> {
                    if (currentIncomingFileOut == null) {
                        Logger.error("Received FILE_END without active file transfer, ignoring.");
                        return;
                    }

                    currentIncomingFileOut.close();

                    // Verify checksum
                    String receivedChecksum = Base64.getEncoder()
                            .encodeToString(currentIncomingDigest.digest());
                    if (!receivedChecksum.equals(expectedIncomingChecksum)) {
                        Logger.error("Checksum mismatch for file " + incomingFileName);
                        System.out.println("File '" + incomingFileName + "' failed checksum verification.");
                        currentIncomingFile.delete();
                    } else {
                        Logger.info("File '" + incomingFileName + "' received successfully and verified.");
                        System.out.println("File '" + incomingFileName + "' received successfully.");
                    }

                    // Reset file transfer state
                    currentIncomingFile = null;
                    currentIncomingFileOut = null;
                    currentIncomingDigest = null;
                    expectedIncomingChecksum = null;
                    incomingFileName = null;
                }

                default -> Logger.debug("Unhandled frame type: " + frame.getType());
            }
        } catch (Exception e) {
            Logger.error("Failed to handle frame", e);
        }
    }



    private void shutdown() {
        executor.shutdownNow();
    }

    private void sendFile(String recipient, Path path) throws IOException, NoSuchAlgorithmException {
        File file = path.toFile();
        if (!file.exists() || !file.isFile()) {
            System.out.println("File not found: " + path);
            return;
        }

        String filename = file.getName();
        long size = file.length();

        Logger.info("Sending file: " + filename + " (" + size + " bytes)");

        // Compute SHA-256 checksum
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[32_768];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        String checksumBase64 = Base64.getEncoder().encodeToString(digest.digest());

        // 1️⃣ FILE_META (recipient|filename|size|checksum)
        String meta = recipient + "|" + filename + "|" + size + "|" + checksumBase64;
        framedConnection.send(new Frame(FrameType.FILE_META, meta.getBytes(StandardCharsets.UTF_8)));

        // 2️⃣ FILE_CHUNK(s)
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[32_768]; // 32 KB chunks
            int read;
            while ((read = in.read(buffer)) != -1) {
                byte[] chunk = new byte[read];
                System.arraycopy(buffer, 0, chunk, 0, read);
                framedConnection.send(new Frame(FrameType.FILE_CHUNK, chunk));
            }
        }

        // 3️⃣ FILE_END
        framedConnection.send(new Frame(FrameType.FILE_END, filename.getBytes(StandardCharsets.UTF_8)));

        Logger.info("File sent successfully: " + filename);
    }



    private void sendFileAccept(String filename) {
        try {
            Logger.info("Accepting file: " + filename);

            framedConnection.send(new Frame(
                    FrameType.FILE_ACCEPT,
                    filename.getBytes(StandardCharsets.UTF_8)
            ));
        } catch (Exception e) {
            Logger.error("Failed to send FILE_ACCEPT", e);
        }
    }

    private void sendFileReject(String filename) {
        try {
            Logger.info("Rejecting file: " + filename);

            framedConnection.send(new Frame(
                    FrameType.FILE_REJECT,
                    filename.getBytes(StandardCharsets.UTF_8)
            ));
        } catch (Exception e) {
            Logger.error("Failed to send FILE_REJECT", e);
        }
    }




    public static void main(String[] args) throws NoSuchAlgorithmException {
        new ChatClient("localhost", 12345, new ClientEncryption()).start();
    }
}
