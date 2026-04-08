# 📁 File Transfer System

## 1. Purpose

The File Transfer System manages secure and reliable file transmission between client and server.

It handles:

- File offering
- Acceptance or rejection
- Chunked file transmission
- Integrity verification

---

## 2. High-Level Flow

1. Sender issues FILE_OFFER
2. Receiver responds with FILE_ACCEPT or FILE_REJECT
3. Sender sends:
    - FILE_START
    - Multiple FILE_CHUNK frames
    - FILE_END

4. Receiver validates checksum

---

## 3. Core Responsibilities

| Responsibility | Description |
|---------------|-------------|
| Chunking | Break large files into manageable pieces |
| Framing | Wrap each piece into protocol frames |
| Integrity | Validate SHA-256 checksum |
| Ordering | Preserve chunk order |
| Error Handling | Abort on failure |

---

## 4. Design Considerations

Why chunking?

- Avoids memory overload
- Supports large file transfers
- Keeps frames predictable in size

Why checksum verification?

- Detects corruption
- Detects tampering
- Ensures file integrity before completion

---

## 5. Security Integration

All file transfer frames:

- Are encrypted using AES-GCM
- Include authenticated frame type binding
- Are rejected if tampered

---

## 6. Architectural Placement

The file transfer system operates:

Above the connection layer  
Below the command layer

It is a specialized protocol implementation built on top of frames.