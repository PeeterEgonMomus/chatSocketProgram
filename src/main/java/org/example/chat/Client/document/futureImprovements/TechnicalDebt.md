# 🛠 Technical Debt & Improvement Areas

## 1. Philosophy

This project intentionally prioritized architectural clarity and adherence to the Open/Closed Principle over rapid feature expansion.

As the system evolved beyond its original academic scope, certain areas were consciously deferred in favor of maintaining architectural integrity.

This document identifies known technical debt and improvement areas.

---

## 2. Identified Technical Debt

### 2.1 Incomplete Registry Activation Method

The method:

````
IncomingTransferRegistry.activate(String transferId)
````

Currently contains no implementation.

It exists for architectural symmetry but should either:

Be properly implemented, or
Be removed to avoid ambiguity


### 2.2 Error Handling Granularity

Some components:

- Throw generic Exception
- Catch and ignore certain failures
- Log without structured error categories

Future improvements:

- Introduce domain-specific exception types
- Define error categories (ProtocolError, CryptoError, IOFailure, etc.)
- Improve failure propagation strategy


### 2.3 Handshake Trust Model

The current handshake:

- Accepts any server public key
- Performs no certificate validation
- Has no pinned trust model

This is acceptable for:

- Controlled environments
- Educational purposes

But should be improved for production use.

### 2.4 File Transfer Backpressure

While the system uses:

- Non-blocking queue
- Dedicated writer thread

There is no:

- Explicit backpressure mechanism
- Flow control beyond chunk size

For very large files or slow disks, memory growth could occur.

### 2.5 Logging System

The logger is functional but minimal.

Possible improvements:

- Log levels with filtering
- Structured logging format
- Configurable output targets
- Log rotation support


### 2.6 Hardcoded Cryptographic Parameters

Parameters such as:

- RSA key size
- AES mode
- Chunk size (4096 bytes)

Are embedded in the code.

Future improvements:

- External configuration
- Centralized security configuration file
- Runtime-adjustable parameters


## 3. Architectural Strength vs Technical Debt

Importantly:

None of the current technical debt violates architectural boundaries.

The system remains:

- Layered
- Extensible
- Modular
- OCP-compliant

Debt exists mostly in implementation refinement — not structural flaws.

## 4. Refactoring Strategy

When addressing technical debt:

- Preserve architectural boundaries
- Avoid cross-layer leakage
- Maintain Open/Closed compliance
- Introduce tests before refactoring

The architecture must remain stable even as implementation evolves.

## 5. Conclusion

The current technical debt is controlled and well-understood.

It reflects conscious trade-offs during growth.

Future improvements will focus on:

- Robustness
- Security hardening
- Observability
- Production-readiness

Without compromising architectural integrity.


---

