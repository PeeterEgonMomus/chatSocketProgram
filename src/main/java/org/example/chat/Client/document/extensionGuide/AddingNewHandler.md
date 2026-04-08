# 📄 `06-extension-guide/AddingNewHandler.md`


# 🎯 Adding a New Frame Handler

## 1. Philosophy

Handlers process incoming frames.

They contain application-level behavior
triggered by specific FrameTypes.

Handlers are extensions to the dispatcher.

---

## 2. Step-by-Step Guide

### Step 1 — Implement FrameHandler

Create a new handler:

```java
public final class NotificationHandler implements FrameHandler {

    @Override
    public void handle(Frame frame) throws Exception {

        if (frame.getType() != FrameType.NOTIFICATION) {
            throw new IllegalArgumentException("Invalid frame type");
        }

        // Decode payload
        // Perform logic
    }
}
```

A handler must:

- Validate frame type
- Decode payload
- Perform isolated logic

It must NOT:

- Access raw sockets
- Perform encryption
- Modify dispatcher core


## Step 2 — Register in Dispatcher

Register the handler:
````
dispatcher.register(FrameType.NOTIFICATION, new NotificationHandler());
````
No modification of dispatcher internals is required.

## 3. Threading Awareness

Handlers may execute on:

- The frame reader thread
- A delegated executor

Handlers should:

- Avoid blocking operations
- Delegate long-running tasks if necessary


## 4. Open/Closed Principle Validation

If adding a handler requires editing existing handler logic,
the architecture has been compromised.

Correct path:

New handler → Register → Done

## 5. Architectural Importance

Handlers represent:

Protocol → Behavior transition

They are the point where structured data
becomes application action.

The dispatcher + handler model ensures:

- Clean extensibility
- Clear separation
- Scalable protocol evolution