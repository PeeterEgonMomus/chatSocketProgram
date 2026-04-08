# 🔄 Runtime Flow

## 1. Purpose

This document describes the dynamic lifecycle of the Chat Client from startup to shutdown.

It explains how components interact during execution.

---

## 2. Startup Sequence

When the client starts, the following steps occur:

1. TCP connection to the server is established.
2. RSA-based handshake is performed.
3. AES session key is negotiated.
4. AES cipher is installed on the connection.
5. Frame handlers are registered in the dispatcher.
6. Command processor is started.
7. Frame reader loop begins listening for incoming frames.

At this point, the client enters operational mode.

---

## 3. Operational Flow

### 3.1 Outbound Message Flow

User Input  
→ CommandStrategy  
→ ClientMessageGateway  
→ Cipher (AES encryption)  
→ FramedChatConnection  
→ TCP Socket

All frames (except handshake frames) are encrypted before transmission.

---

### 3.2 Inbound Message Flow

TCP Socket  
→ FramedChatConnection  
→ Cipher (AES decryption)  
→ FrameDispatcher  
→ FrameHandler

Handlers operate only on fully decrypted frames.

---

### 3.3 File Transfer Flow

Outgoing:

1. `/sendfile` command
2. SEND_FILE_REQUEST
3. FILE_OFFER
4. SEND_FILE_READY (server approval)
5. FILE_START
6. FILE_CHUNK (streamed)
7. FILE_END

Incoming:

1. FILE_OFFER received
2. User accepts or rejects
3. FILE_START
4. FILE_CHUNK (queued)
5. FILE_END
6. Checksum validation

File writing is asynchronous.

---

## 4. Shutdown Flow

1. `/quit` command is issued.
2. Connection is closed.
3. Reader thread exits.
4. Runtime shuts down gracefully.

No abrupt termination is required.

---

## 5. Error Handling Model

- Handshake failure → Abort startup
- Unknown frame → Logged and ignored
- File checksum mismatch → File deleted
- Transfer rejection → Clean state removal

Errors are isolated to prevent cascading failures.