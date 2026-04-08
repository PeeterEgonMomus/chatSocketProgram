# 🧵 Threading Model

## 1. Purpose

This document describes the concurrency design of the Chat Client.

---

## 2. Thread Overview

| Thread | Responsibility |
|--------|---------------|
| Main Thread | Bootstrap + runtime start |
| Frame Reader Thread | Blocking network receive loop |
| Command Thread | Console input processing |
| File Writer Thread | Streaming incoming file chunks |

---

## 3. Concurrency Principles

- Single responsibility per thread
- No shared mutable state without control
- File writes decoupled via queue
- Network reader must never block on disk I/O

---

## 4. Synchronization Strategy

- Registry for transfer state
- Blocking queues for file streaming
- Controlled shutdown signaling

---

## 5. Why This Model Was Chosen

- Prevents I/O blocking cascade
- Keeps command interface responsive
- Avoids unnecessary thread explosion