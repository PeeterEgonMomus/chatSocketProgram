package org.example.chat.Client.file;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry managing incoming file transfers.
 *
 * Maintains two states:
 *
 * 1. Pending Offers (awaiting user decision)
 * 2. Active Transfers (currently streaming)
 *
 * Responsibilities:
 * - Store incoming offers
 * - Activate transfer upon acceptance
 * - Provide lookup for transport handler
 * - Remove completed transfers
 *
 * Thread-safe via ConcurrentHashMap.
 */
public final class IncomingTransferRegistry {

    private final Map<String, IncomingFileState> activeTransfers =
            new ConcurrentHashMap<>();

    // ⭐ NEW: store pending offers
    private final Map<String, PendingOffer> pendingOffers =
            new ConcurrentHashMap<>();


    // ================= PENDING =================

    public void addPending(
            String transferId,
            String filename,
            String checksum
    ) {
        pendingOffers.put(
                transferId,
                new PendingOffer(transferId, filename, checksum)
        );
    }

    public PendingOffer getPending(String transferId) {
        return pendingOffers.get(transferId);
    }

    public void removePending(String transferId) {
        pendingOffers.remove(transferId);
    }


    // ================= ACTIVE =================

    public IncomingFileState activatePendingTransfer(String transferId) throws Exception{

        PendingOffer offer = pendingOffers.remove(transferId);

        if (offer == null) {
            throw new IllegalStateException(
                    "No pending offer: " + transferId
            );
        }

        IncomingFileState state = new IncomingFileState();
        state.start(
                offer.transferId(),
                offer.filename(),
                offer.checksum()
        );

        activeTransfers.put(transferId, state);
        return state;
    }

    public IncomingFileState get(String transferId) {
        return activeTransfers.get(transferId);
    }

    public void remove(String transferId) {
        activeTransfers.remove(transferId);
    }

    public void activate(String transferId) {
    }
}