# 🧠 Command System

## 1. Purpose

The Command System interprets user input and translates it into protocol-level actions.

It represents the bridge between:

Human-readable commands  
and  
Structured protocol frames

---

## 2. Core Abstractions

| Component | Responsibility |
|------------|---------------|
| `CommandProcessor` | Lifecycle of command handling |
| `CommandInputSource` | Abstracts input origin |
| Concrete Commands | Implement specific behavior |

---

## 3. Open/Closed Principle (Core Design Goal)

The Command System is built around one foundational principle:

The system must be open for extension but closed for modification.

Adding a new command must never require changing existing commands.

This is achieved by:

- Interface-based command abstraction
- Encapsulated command logic
- Dependency injection of connection layer

---

## 4. Flow

1. Input source reads raw string
2. Command parser interprets command keyword
3. Matching command object executes
4. Command sends frames through connection layer

---

## 5. Design Characteristics

- No command contains networking code
- No command parses raw bytes
- No command knows encryption details

Each command focuses purely on:

Intent → Frame Creation → Send

---

## 6. Why This Design Matters

This structure allows:

- Future commands without refactoring
- Scripted input sources
- GUI input replacement
- Automated testing of commands

This component directly reflects the project’s commitment to OCP.