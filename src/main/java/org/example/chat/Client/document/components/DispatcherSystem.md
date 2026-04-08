# 🚦 Dispatcher System

## 1. Purpose

The Dispatcher System routes incoming frames to appropriate handlers.

It separates:

Frame reception  
from  
Frame interpretation

---

## 2. Core Responsibility

When a frame is received:

1. Determine its FrameType
2. Lookup matching handler
3. Delegate processing

The dispatcher itself contains no business logic.

---

## 3. Why a Dispatcher?

Without a dispatcher:

- Large conditional blocks would appear
- Frame handling would become tightly coupled
- Extensibility would suffer

With a dispatcher:

- Each frame type has its own handler
- Adding new frame types requires only registration
- Existing logic remains untouched

---

## 4. Open/Closed Principle in Action

The dispatcher demonstrates OCP clearly:

New frame types can be added by:

- Creating a new handler
- Registering it

No modification to core dispatch logic is required.

---

## 5. Threading Considerations

The dispatcher may operate:

- On a dedicated receiver thread
- With asynchronous handler execution

This ensures:

- Non-blocking frame reception
- Clean separation of concerns

---

## 6. Architectural Significance

The dispatcher represents:

The transition from transport-level events  
to  
Application-level behavior

It is the heart of protocol execution.