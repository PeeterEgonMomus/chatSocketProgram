# 🔌 Connection Layer

## 1. Purpose

The Connection Layer abstracts low-level TCP communication and exposes a structured frame-based API.

It ensures:

- Clean separation between transport and application logic
- Deterministic frame parsing
- Encryption boundary enforcement
- Resource lifecycle management

---

## 2. Core Responsibility

The connection layer is responsible for:

- Sending `Frame` objects
- Receiving `Frame` objects
- Handling encryption/decryption
- Managing socket lifecycle
- Protecting against malformed frames

It does not:

- Interpret frame semantics
- Execute commands
- Perform business logic

---

## 3. Key Components

| Component | Responsibility |
|------------|---------------|
| `FramedChatConnection` | High-level connection abstraction |
| `Frame` | Immutable protocol unit |
| `FrameType` | Protocol contract |
| `FrameWriter` | Serializes frames to OutputStream |
| `FrameReader` | Parses frames from InputStream |

---

## 4. Architectural Role

The connection layer represents the **boundary between:**

Application logic  
and  
Network transport

Everything above this layer operates in terms of frames.

Everything below this layer operates in terms of bytes.

---

## 5. Design Principles Applied

- Single Responsibility Principle
- Open/Closed Principle
- Explicit encryption boundary
- Immutable data objects
- Fail-fast validation

---

## 6. Why This Layer Exists

Without this layer:

- Encryption logic would leak into application code
- Byte parsing would be duplicated
- Protocol coupling would increase

By isolating transport concerns, the system remains:

- Extensible
- Testable
- Replaceable