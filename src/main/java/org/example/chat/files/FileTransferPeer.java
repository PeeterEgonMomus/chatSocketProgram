package org.example.chat.files;

import org.example.chat.protocol.Frame;
import org.example.chat.protocol.FrameType;


/**
 * Design choice:
 * Abstraction representing a participant
 * in a file transfer.
 *
 * Allows decoupling from ClientHandler.
 *
 * Instead of tying transfer logic to a concrete class,
 * this interface defines only what is required:
 *
 * - sendEncrypted(...)
 * - send(text message)
 * - getUsername()
 *
 * This improves:
 * - Testability
 * - Decoupling
 * - Future extensibility
 */
public interface FileTransferPeer {

    void sendEncrypted(FrameType type, byte[] payload) throws Exception;

    String getUsername();

    void send(String message) throws Exception;

}
