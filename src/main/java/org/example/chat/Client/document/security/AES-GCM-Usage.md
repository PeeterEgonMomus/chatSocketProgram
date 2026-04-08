# 🔒 AES-GCM Usage Specification

## 1. Cipher Configuration

Algorithm:

AES/GCM/NoPadding

Parameters:

- Key Size: 256-bit
- IV Length: 12 bytes
- Authentication Tag Length: 128 bits

---

## 2. Encryption Process

For each outgoing frame:

1. Generate random 12-byte IV
2. Initialize cipher in ENCRYPT_MODE
3. Add AAD (FrameType name bytes)
4. Encrypt payload
5. Concatenate IV + ciphertext
6. Send combined result as frame payload

Final transmitted payload format:

| IV (12 bytes) | Ciphertext + Auth Tag |

---

## 3. Decryption Process

For each incoming frame:

1. Extract first 12 bytes as IV
2. Extract remainder as ciphertext
3. Initialize cipher in DECRYPT_MODE
4. Add AAD (FrameType name bytes)
5. Decrypt
6. Verify authentication tag

If authentication fails:

- `AEADBadTagException` is thrown
- Frame is rejected
- Error is logged

---

## 4. Security Properties

AES-GCM provides:

- Confidentiality
- Integrity
- Authentication
- Replay detection (per-session basis)

---

## 5. IV Requirements

Each encryption call:

- Generates a fresh random IV
- Never reuses IV with same key

IV reuse in GCM would catastrophically break security.

The implementation guarantees unique IV per frame.

---

## 6. Why 12-Byte IV?

12 bytes (96 bits) is:

- NIST-recommended length
- Optimized for GCM performance
- Minimizes GHASH precomputation cost