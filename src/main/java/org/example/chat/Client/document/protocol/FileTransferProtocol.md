# 📁 File Transfer Protocol

## 1. Purpose

This document defines the structured protocol used for secure file transfer between client and server.

File transfer is fully frame-based and operates in explicit stages.

---

## 2. Outgoing Transfer Flow

### Stage 1 – Request

Client sends:

SEND_FILE_REQUEST  
Payload:
- transferId
- recipient

---

### Stage 2 – Offer

Client sends:

FILE_OFFER  
Payload:
- transferId
- filename
- size
- checksum (SHA-256, Base64)

---

### Stage 3 – Decision

Recipient sends:

FILE_ACCEPT  
or  
FILE_REJECT

---

### Stage 4 – Authorization

Server sends:

SEND_FILE_READY

Only after this frame may upload begin.

---

### Stage 5 – Streaming

Client sends:

FILE_START  
FILE_CHUNK (multiple)  
FILE_END

FILE_CHUNK payload:

- transferId
- chunkIndex
- chunkLength
- rawBytes

---

## 3. Incoming Transfer Flow

1. FILE_OFFER received
2. Offer stored as pending
3. User accepts
4. Transfer activated
5. FILE_START
6. FILE_CHUNK queued
7. FILE_END
8. Checksum verification

---

## 4. Checksum Validation

Incoming transfers:

- Compute SHA-256 during write
- Compare with expected checksum
- Delete file on mismatch

This prevents corrupted or tampered files from persisting.

---

## 5. Concurrency Design

File writing is asynchronous:

- FILE_CHUNK enqueues data
- Dedicated writer thread consumes queue
- Network thread never blocks on disk I/O

---

## 6. Protocol Guarantees

The file protocol ensures:

- Explicit negotiation
- Explicit approval
- Controlled streaming
- Integrity verification
- Clean failure handling

No implicit or uncontrolled file transmission occurs.