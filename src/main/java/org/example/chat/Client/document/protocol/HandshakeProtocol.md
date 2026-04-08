# 🔐 Handshake Protocol

## 1. Purpose

This document describes the secure session establishment between client and server.

The system uses hybrid cryptography:

- RSA (2048-bit) for key exchange
- AES-256-GCM for session encryption

---

## 2. Handshake Goals

The handshake must:

1. Establish a shared AES session key
2. Prevent passive eavesdropping
3. Prevent tampering
4. Prepare encrypted channel for all future frames

---

## 3. Handshake Flow

### Step 1 – Client Hello

Client connects via TCP.

---

### Step 2 – RSA Public Key Exchange

Server provides its RSA public key.

---

### Step 3 – AES Session Key Generation

Client:

- Generates random AES-256 key
- Encrypts it with server RSA public key
- Sends encrypted key to server

---

### Step 4 – Session Activation

Server decrypts AES key.

Both sides now share:

- AES session key
- GCM mode configuration

Handshake is complete.

---

## 4. Security Properties

| Property | Mechanism |
|----------|-----------|
| Confidentiality | AES-256-GCM |
| Integrity | GCM authentication tag |
| Key exchange security | RSA encryption |
| Replay protection | GCM nonce usage |

---

## 5. Post-Handshake Rule

After handshake:

- All frames must be encrypted
- FrameType is bound into AAD
- Plaintext frames are rejected

The handshake is the only unencrypted phase.