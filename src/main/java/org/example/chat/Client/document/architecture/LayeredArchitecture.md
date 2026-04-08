# 🧱 Layered Architecture

## 1. Purpose

This document describes the static structural decomposition of the Chat Client into layers and defines the dependency rules between them.

---

## 2. Layer Overview

The system is divided into the following layers:

| Layer | Responsibility |
|-------|---------------|
| Bootstrap | Application startup and wiring |
| Runtime | Lifecycle orchestration |
| Connection | TCP + frame encoding/decoding |
| Protocol | Frame dispatching and routing |
| Crypto | Encryption and handshake |
| Command | User command parsing and execution |
| File | File transfer lifecycle |
| Gateway | Outbound abstraction |
| Handler | Inbound frame logic |

---

## 3. Layer Diagram
Bootstrap
↓
Runtime
↓
Connection
↓
Protocol
↓
Handlers

Crypto (cross-cutting but isolated)
Command (parallel user interaction layer)
File (parallel transfer subsystem)


---

## 4. Dependency Rules

1. Dependencies flow inward only.
2. Lower layers never depend on higher layers.
3. Crypto does not depend on runtime.
4. Handlers do not depend on connection internals.
5. Commands do not perform encryption directly.

---

## 5. Rationale

- Ensures OCP adherence
- Enables subsystem replacement
- Prevents cyclic dependencies
- Improves testability