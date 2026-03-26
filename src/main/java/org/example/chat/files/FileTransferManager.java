package org.example.chat.files;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Design choice:
 * Central registry and lifecycle manager
 * for all active file transfers.
 *
 * ---------------------------------------------------------
 * Architectural Role
 * ---------------------------------------------------------
 *
 * FileTransferManager is the Coordinator of the subsystem.
 *
 * It owns:
 * - Transfer storage (ConcurrentHashMap)
 * - Transfer creation
 * - Transfer removal
 * - Bulk abort operations
 * - Delivery executor access
 *
 * It does NOT:
 * - Implement transfer logic (ActiveFileTransfer does)
 *
 * ---------------------------------------------------------
 * Concurrency Model:
 * ---------------------------------------------------------
 *
 * - ConcurrentHashMap ensures thread-safe storage.
 * - No global locking required.
 * - Executor handles asynchronous delivery.
 *
 * ---------------------------------------------------------
 * Safety Guarantees:
 * ---------------------------------------------------------
 *
 * abort():
 * - Ensures partial transfers are cleaned.
 *
 * abortTransfersForPeer():
 * - Critical for handling disconnects.
 *
 * Prevents:
 * - Resource leaks
 * - Orphaned transfers
 */
public final class FileTransferManager {

    private final ConcurrentHashMap<String, ActiveFileTransfer> transfers =
            new ConcurrentHashMap<>();

    private final ExecutorService executor;


    public FileTransferManager(ExecutorService executor) {
        this.executor = executor;
    }



    /* ================= CREATE ================= */

    public ActiveFileTransfer createTransfer(
            String transferId,
            FileTransferPeer sender,
            FileTransferPeer recipient) {

        ActiveFileTransfer transfer =
                new ActiveFileTransfer(
                        transferId,
                        sender,
                        recipient,
                        this
                );

        transfers.put(transferId, transfer);

        return transfer;
    }



    /* ================= LOOKUP ================= */

    public ActiveFileTransfer getById(String id) {
        return transfers.get(id);
    }


    /* ================= REMOVE ================= */

    public void remove(String id) {
        transfers.remove(id);
    }


    /* ================= SAFETY ================= */

    /**
     * Abort transfer safely and remove it.
     * Call this on disconnects.
     */
    public void abort(String id) {

        ActiveFileTransfer transfer = transfers.remove(id);

        if (transfer != null) {
            transfer.abort();
        }
    }

    public void abortTransfersForPeer(FileTransferPeer peer) {

        transfers.values().removeIf(transfer -> {

            boolean involved =
                    transfer.getSender() == peer ||
                            transfer.getRecipient() == peer;

            if (involved) {
                transfer.abort();
            }

            return involved;
        });
    }



    /* ================= DEBUG (Optional but VERY Useful) ================= */

    public int activeCount() {
        return transfers.size();
    }

    ExecutorService executor() {
        return executor;
    }
}
