# 🔐 Cryptography Model

## 1. Purpose

This document defines the cryptographic architecture used by the Chat Client.

The client implements a **hybrid cryptography model** combining:

- Asymmetric encryption (RSA) for secure key exchange
- Symmetric encryption (AES-256-GCM) for session communication
- Authenticated encryption with Additional Authenticated Data (AAD)

The goal is to provide:

- Confidentiality
- Integrity
- Authenticity of frame types
- Protection against tampering

---

## 2. Hybrid Encryption Overview

The system uses a two-phase security model:

### Phase 1 — Asymmetric Key Exchange

- Server sends RSA public key
- Client generates:
    - RSA key pair
    - AES session key (256-bit)
- Client encrypts AES key with server public key
- Server decrypts AES key using private key

After this phase:

A shared AES session key exists between client and server.

---

### Phase 2 — Symmetric Secure Communication

All non-handshake frames are encrypted using:

**AES-256-GCM**

Properties:

- Confidentiality via AES
- Integrity via GCM authentication tag
- FrameType binding via AAD

---

## 3. Cryptographic Components

| Component | Algorithm | Purpose |
|------------|-----------|----------|
| Key Exchange | RSA 2048 | Secure session key negotiation |
| Session Encryption | AES-256-GCM | Authenticated encryption |
| Checksum (Files) | SHA-256 | File integrity validation |

---

## 4. Design Decisions

### 4.1 Why Hybrid Encryption?

RSA is computationally expensive.  
AES is fast and secure for bulk data.

Using RSA only for key exchange and AES for session data provides:

- Strong security
- High performance
- Standard industry practice

---

### 4.2 Why AES-GCM?

AES-GCM provides:

- Encryption
- Authentication
- Integrity verification

In a single operation.

It prevents:

- Message tampering
- Ciphertext modification
- Bit-flipping attacks

---

### 4.3 Why Bind FrameType as AAD?

The `FrameType` enum is added as Additional Authenticated Data (AAD).

This ensures:

A ciphertext for one frame type cannot be replayed or interpreted as another type.

This prevents protocol confusion attacks.

---

## 5. Security Guarantees

The system guarantees:

- Encrypted payload confidentiality
- Authenticated message integrity
- Detection of tampered frames
- Prevention of unauthorized frame type substitution

The system does not guarantee:

- Server identity verification (no PKI)
- Forward secrecy (no Diffie-Hellman)
- Protection against compromised server private key

These are explicitly outside project scope.

---

## 6. Encryption Boundary

Handshake frames:

- HANDSHAKE_SERVER_KEY
- HANDSHAKE_CLIENT_KEY
- HANDSHAKE_AES_KEY
- HANDSHAKE_OK

Are transmitted in plaintext.

All other frames are encrypted.

---

## 7. Threat Model Assumptions

The model assumes:

- Passive eavesdroppers exist
- Active tampering is possible
- Network is untrusted
- Server private key is secure

It does not assume:

- Trusted certificate authorities
- TLS infrastructure
- Hardware secure modules