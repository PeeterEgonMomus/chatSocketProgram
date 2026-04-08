# 📘 Chat Client – System Overview

---

## 1. Purpose

The Chat Client is a secure, frame-based TCP client responsible for:

- Establishing a secure session with the chat server
- Sending and receiving encrypted chat messages
- Managing encrypted file transfers
- Processing user commands via a console interface
- Dispatching protocol frames to appropriate handlers

The client is designed with modular architecture, strong separation of concerns, and explicit protocol boundaries.

---

## 2. High-Level Responsibilities

The client is responsible for:

- Establishing a TCP connection to the server
- Performing an RSA-based handshake
- Negotiating a symmetric AES session key
- Encrypting and decrypting all post-handshake communication
- Managing asynchronous frame processing
- Providing a command-driven user interface
- Handling file transfer negotiation and streaming

### The client does **not**:

- Store persistent user data
- Maintain authentication credentials
- Manage server-side state
- Perform user account management

---

## 3. Architectural Principles

The client architecture follows these core principles:

---

### 3.1 Layered Design

The system is divided into clearly defined layers:

| Layer      | Responsibility                  |
|------------|--------------------------------|
| Bootstrap  | Application startup            |
| Runtime    | Orchestrates lifecycle         |
| Connection | Raw TCP + framing              |
| Protocol   | Frame dispatching              |
| Crypto     | Encryption logic               |
| Command    | User command system            |
| File       | File transfer subsystem        |
| Gateway    | Outbound abstraction           |
| Handler    | Inbound frame logic            |

Each layer has a single responsibility and communicates only through defined interfaces.

---

### 3.2 Explicit Protocol Boundaries

The client operates entirely through `Frame` objects.

There is:

- No raw socket logic outside the connection layer
- No encryption logic outside the crypto layer
- No protocol logic inside the runtime

This ensures:

- Predictability
- Testability
- Clear data flow

---

### 3.3 Dependency Direction

Dependencies flow inward:

Higher-level components orchestrate lower-level components.  
Lower-level components never depend on runtime logic.

---

### 3.4 Thread Isolation

The client uses dedicated threads for:

- Frame reading (network I/O)
- Command input processing
- File writing (asynchronous file streaming)

This ensures:

- Non-blocking file reception
- Responsive command interface
- Clean shutdown behavior

---

## 4. Runtime Lifecycle

When the client starts:

1. TCP connection is established
2. Handshake protocol executes (RSA → AES)
3. AES cipher is installed on the connection
4. Frame handlers are registered
5. Command processor starts
6. Frame reader loop begins

From that point onward:

- Incoming frames are dispatched
- Outgoing frames are encrypted automatically
- Commands are translated into protocol frames

---

## 5. Security Model

The client implements a hybrid encryption model:

- RSA (2048-bit) for key exchange
- AES-256-GCM for session encryption
- FrameType bound via AAD (Additional Authenticated Data)

### Security properties:

- Confidentiality (AES encryption)
- Integrity (GCM authentication tag)
- Type-binding (FrameType included in AAD)
- Protection against tampering

Handshake frames are transmitted in plaintext.  
All subsequent frames are encrypted.

---

## 6. File Transfer Model

File transfers operate in stages:

1. `SEND_FILE_REQUEST`
2. `FILE_OFFER`
3. `FILE_ACCEPT` / `FILE_REJECT`
4. `SEND_FILE_READY` (server authorization)
5. `FILE_START`
6. `FILE_CHUNK` (streamed)
7. `FILE_END`

Incoming transfers:

- Use a non-blocking queue
- Are written on a dedicated writer thread
- Are checksum-validated (SHA-256)

This prevents:

- Frame reader blocking
- Memory overflow
- Corrupt file persistence

---

## 7. Extensibility

The system is intentionally extensible:

- New commands can be added via `CommandStrategy`
- New frame types can be registered in `FrameDispatcher`
- New handlers can be attached without modifying runtime
- Encryption layer can be replaced via `ClientCipher`

---

## 8. Design Patterns Used

| Pattern    | Location                    |
|------------|----------------------------|
| Strategy   | Command system             |
| Dispatcher | FrameDispatcher            |
| Gateway    | ClientMessageGateway       |
| Builder    | CommandRegistryBuilder     |
| State      | File transfer states       |
| Adapter    | Cipher wrapping Crypto     |

The system favors composition over inheritance.

---

## 9. Non-Goals

The client is not intended to:

- Scale horizontally
- Support multiple concurrent server connections
- Persist file transfer history
- Provide GUI functionality

It is a secure, console-based reference implementation.

---

## 10. Summary

The Chat Client is a modular, encrypted, frame-driven TCP application built with clear architectural separation and explicit protocol modeling.

It demonstrates:

- Secure session negotiation
- Authenticated encryption
- Event-driven frame dispatching
- Asynchronous file streaming
- Strategy-based command handling

It serves both as a functional secure chat client and as a reference architecture for structured network applications.