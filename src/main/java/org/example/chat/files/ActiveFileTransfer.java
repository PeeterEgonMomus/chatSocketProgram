package org.example.chat.files;

import org.example.chat.PendingFile;
import org.example.chat.util.Logger;


/**
 * Design choice:
 * Stateful transfer controller (Transfer State Machine).
 *
 * ActiveFileTransfer represents ONE logical file transfer
 * between exactly two peers.
 *
 * ---------------------------------------------------------
 * Architectural Role
 * ---------------------------------------------------------
 *
 * This is the Domain Object of the File Transfer subsystem.
 *
 * It owns:
 * - Transfer identity
 * - Sender and recipient
 * - Current state
 * - Upload session
 * - Pending delivery
 *
 * It does NOT:
 * - Manage storage of all transfers (FileTransferManager does that)
 * - Perform encryption (ClientHandler does that)
 *
 * ---------------------------------------------------------
 * Core Concept:
 * ---------------------------------------------------------
 *
 * This class implements a finite state machine.
 *
 * State transitions:
 *
 * INIT
 *   ↓
 * WAITING_FOR_RECIPIENT
 *   ↓ (accept)
 * UPLOADING
 *   ↓
 * OFFERED
 *   ↓
 * DELIVERING
 *   ↓
 * COMPLETED | FAILED | REJECTED | ABORTED
 *
 * Every public method validates state before acting.
 *
 * ---------------------------------------------------------
 * Concurrency Model:
 * ---------------------------------------------------------
 *
 * - Most mutating operations are synchronized.
 * - Delivery runs asynchronously via executor.
 * - State changes are controlled and logged.
 *
 * This prevents race conditions during upload.
 *
 * ---------------------------------------------------------
 * Design Strength:
 * ---------------------------------------------------------
 *
 * All transfer rules are centralized here.
 * No other class is allowed to bypass state validation.
 *
 * This protects protocol correctness.
 */
public final class ActiveFileTransfer {

    public enum State {
        INIT,
        WAITING_FOR_RECIPIENT,
        UPLOADING,
        OFFERED,
        DELIVERING,
        COMPLETED,
        FAILED,
        REJECTED,
        ABORTED
    }

    private final String id;
    private final FileTransferPeer sender;
    private final FileTransferPeer recipient;
    private final FileTransferManager manager;

    private FileTransferSession uploadSession;
    private PendingFile pendingFile;
    private FileDescriptor descriptor;

    private volatile State state = State.INIT;

    public ActiveFileTransfer(
            String id,
            FileTransferPeer sender,
            FileTransferPeer recipient,
            FileTransferManager manager
    ) {
        this.id = id;
        this.sender = sender;
        this.recipient = recipient;
        this.manager = manager;
    }

    public String getId() { return id; }
    public FileTransferPeer getSender() { return sender; }
    public FileTransferPeer getRecipient() { return recipient; }

    /* =========================================================
     * Descriptor Registration
     * ========================================================= */

    public synchronized void registerDescriptor(FileDescriptor descriptor) {
        assertState(State.WAITING_FOR_RECIPIENT);
        this.descriptor = descriptor;
    }

    /* =========================================================
     * Upload Lifecycle
     * ========================================================= */

    public synchronized void startUploadSession() throws Exception {

        assertState(State.UPLOADING);

        if (descriptor == null)
            throw new IllegalStateException("Descriptor not registered");

        if (uploadSession != null)
            throw new IllegalStateException("Upload already started");

        uploadSession = new FileTransferSession();

        uploadSession.start(
                sender.getUsername(),
                descriptor.getFilename(),
                descriptor.getSize(),
                descriptor.getChecksumBase64()
        );

        Logger.info("Upload session initialized for transfer " + id);
    }

    public synchronized void acceptChunk(int index, byte[] data) throws Exception {
        assertState(State.UPLOADING);

        if (uploadSession == null)
            throw new IllegalStateException("Upload session not started");

        uploadSession.acceptChunk(index, data);
    }

    public synchronized void finishUpload() throws Exception {

        assertState(State.UPLOADING);

        if (uploadSession == null)
            throw new IllegalStateException("Upload session not started");

        uploadSession.finish();

        pendingFile = new PendingFile(
                id,
                sender,
                recipient,
                descriptor,
                uploadSession.getFile()
        );

        state = State.OFFERED;
    }

    /* =========================================================
     * Accept / Reject
     * ========================================================= */

    public void accept() {

        assertState(State.OFFERED);
        state = State.DELIVERING;

        manager.executor().submit(() -> {
            try {
                pendingFile.startDelivery();
                state = State.COMPLETED;
                Logger.info("Transfer " + id + " completed");
            } catch (Exception e) {
                state = State.FAILED;
                Logger.error("Transfer " + id + " failed", e);
            } finally {
                manager.remove(id);
            }
        });
    }

    public void reject() {

        assertState(State.OFFERED);

        cleanup();
        state = State.REJECTED;
        manager.remove(id);
    }

    /* =========================================================
     * Cleanup
     * ========================================================= */

    public void abort() {
        state = State.ABORTED;
        if (uploadSession != null) uploadSession.abort();
        cleanup();
    }

    private void cleanup() {
        if (pendingFile != null) pendingFile.cleanup();
    }

    private synchronized void assertState(State expected) {
        if (state != expected) {
            throw new IllegalStateException(
                    "Transfer " + id + " expected " + expected + " but was " + state
            );
        }
    }

    public synchronized State getState() {
        return state;
    }

    public synchronized void setState(State newState) {
        Logger.info("Transfer " + id + " state change: " + state + " -> " + newState);
        state = newState;
    }

    public synchronized void assertAndSetState(State expected, State next) {
        assertState(expected);
        setState(next);
    }
}
