# 🛡 Trust Model

## 1. Trust Boundaries

The system defines the following trust zones:

**Trusted:**
- Client runtime
- Server runtime
- AES session key
- Server private key

**Untrusted:**
- Network
- External clients
- Any intercepted traffic

---

## 2. Server Trust Assumption

The client trusts:

The first public key received from the server.

There is:

- No certificate validation
- No CA verification
- No public key pinning

This makes the model vulnerable to:

- Man-in-the-middle attacks during first connection

This is acceptable for:

- Controlled environments
- Educational purposes
- Closed network systems

---

## 3. Security Goals

The system protects against:

- Passive eavesdropping
- Frame tampering
- Payload modification
- Frame-type substitution
- Corrupt file delivery

---

## 4. Known Limitations

The system does not provide:

- Perfect Forward Secrecy
- Identity validation via PKI
- Replay attack prevention across sessions
- DoS mitigation

---

## 5. Future Improvements (Optional)

Possible enhancements:

- ECDHE key exchange for forward secrecy
- Certificate-based authentication
- Key pinning
- Session resumption
- Rate limiting