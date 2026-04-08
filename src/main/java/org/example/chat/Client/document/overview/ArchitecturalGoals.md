# 🎯 Architectural Goals

---

## 1. Purpose of This Document

This document defines the architectural objectives that guided the design of the Chat Client.

It explains not only *how* the system is structured, but *why* it was designed this way.

The project began as a school assignment.  
However, from the beginning, the primary goal was not simply to complete the assignment — it was to grow as a software engineer.

The guiding principle was clear:

> Adhere strictly to the Open/Closed Principle (OCP).  
> Extend behavior without modifying stable code.

Every architectural decision that followed — layering, strategy patterns, dispatchers, gateways, isolation of crypto, modular file handling — exists in service of that principle.

Over time, the project evolved beyond its original scope.  
It became a structured, secure, modular system — and a representation of deliberate software design.

This client is both:
- A functional secure chat implementation
- A demonstration of architectural discipline and growth

---

## 2. Foundational Design Philosophy

The single core principle at the start of the project was:

### The Open/Closed Principle (OCP)

> Software entities should be open for extension, but closed for modification.

This principle influenced:

- The CommandStrategy system
- The FrameDispatcher
- The Gateway abstraction
- The Cipher abstraction
- The separation of handlers
- The layered runtime orchestration

Instead of adding logic by editing central components, new behavior is introduced by adding new components.

OCP was not applied as a pattern —  
it became the architectural backbone of the entire system.

All other design decisions emerged naturally from the attempt to consistently respect this principle.

---

## 3. Core Architectural Objectives

Beyond OCP, the system aims to achieve:

1. Security by design
2. Strict separation of concerns
3. Explicit protocol modeling
4. Predictable data flow
5. Extensibility without modification
6. Controlled concurrency
7. Testability
8. Failure containment

These objectives reinforce the foundational philosophy rather than replace it.

---

## 4. Security by Design

Security is not layered on top — it is embedded into the structure.

The client:

- Uses hybrid encryption (RSA + AES-GCM)
- Authenticates encrypted frames
- Binds message type to authenticated data (AAD)
- Separates handshake from session communication
- Avoids exposing raw socket or cipher logic outside defined layers

Security responsibilities are isolated and composable.

Goal:

> Sensitive logic must be explicit, isolated, and replaceable without architectural collapse.

---

## 5. Strict Separation of Concerns

Each subsystem has a singular, clearly defined responsibility.

Examples:

- Connection layer handles only TCP and framing.
- Crypto layer handles only encryption and decryption.
- Protocol layer handles only dispatching.
- Command layer handles user intent interpretation.
- File layer handles transfer lifecycle management.

No component performs mixed responsibilities.

This separation makes adherence to OCP possible.

Goal:

> Changing one subsystem must not require rewriting unrelated layers.

---

## 6. Explicit Protocol Modeling

All communication is represented as structured `Frame` objects.

There are:

- No hidden message formats
- No raw socket usage outside the connection layer
- No encryption logic outside the cipher layer

Goal:

> The protocol must be visible, explicit, and structurally enforced.

---

## 7. Predictable Data Flow

The system follows strict directional flow.

Outbound: Command → Gateway → Cipher → Connection → Socket

Inbound: Socket → Connection → Cipher → Dispatcher → Handler


There are no circular dependencies.

Goal:

> Every byte entering or leaving the system must follow a deterministic path.

---

## 8. Extensibility Without Modification

The architecture allows extension without altering stable code.

Examples:

- New commands via `CommandStrategy`
- New frame types via `FrameDispatcher`
- New handlers via registration
- Cipher replacement via `ClientCipher`

The runtime does not require modification when features are added.

Goal:

> New functionality should be introduced through composition, not invasive change.

---

## 9. Controlled Concurrency

Concurrency is minimal and deliberate.

Dedicated threads:

- Frame reader
- Command processor
- File writer

Threads are isolated by responsibility.

Goal:

> Concurrency should improve responsiveness without introducing hidden coupling.

---

## 10. High Cohesion, Low Coupling

The system favors:

- Interface-driven design
- Composition over inheritance
- Replaceable components

Examples:

- `CommandStrategy`
- `FrameHandler`
- `ClientCipher`
- `CommandInputSource`

Goal:

> Components should be swappable without architectural instability.

---

## 11. Failure Containment

Errors are localized.

- File transfer failures do not crash the frame reader.
- Command errors do not terminate runtime.
- Handshake failure prevents startup rather than corrupting state.

Goal:

> Failure in one subsystem must not destabilize the entire system.

---

## 12. Evolution Beyond Scope

Although originally a school project, the system evolved significantly beyond its academic requirements.

It became:

- A structured secure networking application
- A practical demonstration of architectural principles
- A personal benchmark for growth in software engineering

The project continues to evolve and serves as:

- A learning platform
- A reference architecture
- A demonstration of disciplined programming

---

## 13. Summary

The Chat Client architecture prioritizes:

- Open/Closed Principle adherence
- Security
- Explicit protocol modeling
- Deterministic data flow
- Isolation of responsibilities
- Controlled extensibility

What began as a school assignment became a deliberate exploration of professional software design.

This system reflects not only functional requirements, but architectural intent and personal growth as an engineer.