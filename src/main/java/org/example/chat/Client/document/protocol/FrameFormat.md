# 📦 Frame Format Specification

## 1. Purpose

This document defines the binary frame format used for all client-server communication.

The protocol is frame-based and length-prefixed.  
All communication occurs through `Frame` objects.

---

## 2. Frame Structure (Transport Layer)

Each frame transmitted over TCP follows this structure:

| Field | Type | Description |
|-------|------|------------|
| length | int (4 bytes) | Total payload length (excluding this field) |
| type | int (4 bytes) | FrameType identifier |
| payload | byte[] | Frame payload |

Binary layout:

| Offset (bytes) | Size (bytes) | Field   | Description                                                  |
| -------------- | ------------ | ------- | ------------------------------------------------------------ |
| 0              | 4            | length  | Total size of `type + payload` (does not include this field) |
| 4              | 4            | type    | Integer representation of `FrameType`                        |
| 8              | variable     | payload | Frame-specific data                                          |


- `length` includes both `type` and `payload`
- `type` maps to a `FrameType` enum value

---

## 3. FrameType Enumeration

Each frame type represents a semantic protocol action.

Examples:

- CHAT
- FILE_OFFER
- FILE_ACCEPT
- FILE_REJECT
- SEND_FILE_REQUEST
- SEND_FILE_READY
- FILE_START
- FILE_CHUNK
- FILE_END

Frame types are protocol-level contracts and must remain stable.

---

## 4. Payload Encoding

Payloads are encoded using `DataOutputStream` conventions:

- UTF strings → writeUTF
- Integers → writeInt
- Long → writeLong
- Raw bytes → write(byte[])

All structured payloads must follow strict ordering.

---

## 5. Encryption Boundary

Handshake frames are transmitted in plaintext.

All frames after handshake are encrypted before transport.

Encryption is applied to:

- Frame payload
- Authentication tag
- Additional Authenticated Data (AAD includes FrameType)

---

## 6. Design Rationale

The frame-based model provides:

- Clear protocol boundaries
- Transport independence
- Predictable parsing
- Easy extension via new FrameType values

The system never sends raw text over TCP.
All communication is structured and typed.