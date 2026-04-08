# 🔒 Secure Frame Protocol

## 1. Purpose

This document defines how frames are encrypted and authenticated after the handshake phase.

---

## 2. Encryption Model

The system uses:

AES-256 in GCM mode

GCM provides:

- Encryption
- Integrity
- Authentication

---

## 3. Encryption Process (Outbound)

When sending a frame:

1. Frame payload is serialized
2. FrameType is converted to integer
3. FrameType is included as AAD (Additional Authenticated Data)
4. AES-GCM encrypts the payload
5. Authentication tag is appended
6. Encrypted frame is sent

---

## 4. Decryption Process (Inbound)

When receiving a frame:

1. Extract encrypted payload
2. Extract FrameType
3. Use FrameType as AAD
4. Decrypt with AES-GCM
5. Verify authentication tag
6. Pass decrypted payload to dispatcher

If authentication fails:

- Frame is rejected
- Connection may be terminated

---

## 5. AAD Binding

FrameType is bound as AAD.

This ensures:

- Payload cannot be replayed under different FrameType
- Type tampering is detected

---

## 6. Security Guarantees

AES-GCM ensures:

- Confidentiality
- Integrity
- Authenticity
- Protection against bit-flipping attacks

The protocol prevents:

- Payload tampering
- Type spoofing
- Undetected modification