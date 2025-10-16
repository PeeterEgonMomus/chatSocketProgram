package org.example.chat.Client;

import org.example.chat.util.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileTransferClient {
    private final String host;
    private final int port;
    private final ClientCrypto crypto;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public FileTransferClient(String host, int port, ClientCrypto crypto) {
        this.host = host;
        this.port = port;
        this.crypto = crypto;
    }

    // -------------------------------------------------
    // Sending logic (sends header including encrypted-size and checksum of encrypted bytes)
    // -------------------------------------------------
    public void sendFile(Path filePath, String recipient) {
        executor.submit(() -> {
            try (SocketFileConnection conn = new SocketFileConnection(new Socket(host, port + 1))) {
                Logger.info("Connected to file transfer socket for upload");

                // Read plain bytes
                byte[] plain = Files.readAllBytes(filePath);
                Logger.info("Read file " + filePath.getFileName() + " (" + plain.length + " bytes)");

                // Encrypt the whole file (IV + ciphertext)
                byte[] encrypted = encryptFile(plain);
                Logger.info("Encrypted file into " + encrypted.length + " bytes (IV + ciphertext)");

                // Compute SHA-256 checksum over the encrypted bytes so server can validate without decrypting
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] checksumBytes = md.digest(encrypted);
                String checksumHex = HexFormat.of().formatHex(checksumBytes);

                // Send metadata header: UPLOAD:recipient:filename:size:checksum\n
                String header = "UPLOAD:" + recipient + ":" + filePath.getFileName() + ":" +
                        encrypted.length + ":" + checksumHex + "\n";
                conn.sendBytes(header.getBytes("UTF-8"));
                Logger.info("Sent upload header -> recipient=" + recipient +
                        ", filename=" + filePath.getFileName() + ", enc-size=" + encrypted.length +
                        ", checksum=" + checksumHex);

                // Stream encrypted bytes
                final int CHUNK = 8192;
                int offset = 0;
                while (offset < encrypted.length) {
                    int len = Math.min(CHUNK, encrypted.length - offset);
                    conn.sendBytes(java.util.Arrays.copyOfRange(encrypted, offset, offset + len));
                    offset += len;
                }

                Logger.info("File sent successfully.");
            } catch (Exception e) {
                Logger.error("File transfer failed", e);
            }
        });
    }

    // -------------------------------------------------
    // Receiving logic (unchanged protocol: READY:sender:filename\n -> server streams encrypted bytes of file)
    // -------------------------------------------------
    public void receiveFile(String fileName, int fileSize) {
        executor.submit(() -> {
            try (SocketFileConnection conn = new SocketFileConnection(new Socket(host, port + 1))) {
                Logger.info("Connected to file transfer socket for download");

                // Send ACK to server saying “I’m ready to receive”
                String readyMsg = "READY:" + fileName + "\n";
                conn.sendBytes(readyMsg.getBytes("UTF-8"));
                Logger.info("Sent READY for file: " + fileName);

                // Read exactly fileSize bytes of encrypted payload
                byte[] encrypted = new byte[fileSize];
                int totalRead = 0;
                while (totalRead < fileSize) {
                    int n = conn.receiveBytes(encrypted, totalRead, fileSize - totalRead);
                    if (n == -1) break;
                    totalRead += n;
                }

                Logger.info("Received " + totalRead + " encrypted bytes for file " + fileName);

                if (totalRead != fileSize) {
                    Logger.error("Expected " + fileSize + " bytes but received " + totalRead, null);
                }

                byte[] decrypted = decryptFile(encrypted, totalRead);

                Path savePath = Path.of("downloads").resolve(fileName);
                Files.createDirectories(savePath.getParent());
                Files.write(savePath, decrypted);

                Logger.info("File saved to " + savePath.toAbsolutePath());
            } catch (Exception e) {
                Logger.error("File receive failed", e);
            }
        });
    }


    // helper to allow partial reads into buffer
    // added overloaded receiveBytes on SocketFileConnection below (if you already have only single-arg receiveBytes, adapt)
    // -------------------------------------------------
    // Encryption helpers (full-file encrypt/decrypt)
    // -------------------------------------------------
    private byte[] encryptFile(byte[] fileBytes) throws Exception {
        if (!crypto.isAESReady())
            throw new IllegalStateException("AES not ready for file encryption");

        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, ((ClientEncryption) crypto).getAESKey(), spec);

        byte[] encrypted = cipher.doFinal(fileBytes);

        ByteArrayOutputStream out = new ByteArrayOutputStream(12 + encrypted.length);
        out.write(iv);
        out.write(encrypted);
        return out.toByteArray();
    }

    private byte[] decryptFile(byte[] encryptedWithIv, int validLength) throws Exception {
        if (!crypto.isAESReady())
            throw new IllegalStateException("AES not ready for file decryption");

        if (validLength < 12) throw new IllegalArgumentException("Data too short for IV+ciphertext");

        byte[] iv = new byte[12];
        System.arraycopy(encryptedWithIv, 0, iv, 0, 12);

        byte[] cipherText = new byte[validLength - 12];
        System.arraycopy(encryptedWithIv, 12, cipherText, 0, cipherText.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, ((ClientEncryption) crypto).getAESKey(), spec);

        return cipher.doFinal(cipherText);
    }
}
