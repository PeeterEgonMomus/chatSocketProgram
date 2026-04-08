# 📄 `06-extension-guide/AddingNewFrameType.md`


# 🧩 Adding a New FrameType

## 1. Philosophy

FrameType represents protocol-level contracts.

Adding a new FrameType extends the protocol.

It must be done carefully and consistently.

---

## 2. Step-by-Step Guide

### Step 1 — Add Enum Entry

Add the new type inside the FrameType enum:


NEW_FEATURE_EVENT

FrameType values must remain stable once released.

Never reorder existing values if ordinal mapping is used.

### Step 2 — Define Payload Structure

Decide:

- What fields are required?
- In what order?
- What encoding format?

All structured payloads must:

- Use strict field ordering
- Follow DataOutputStream encoding conventions
- Be documented


### Step 3 — Update Both Client and Server

Protocol changes require:

- Matching support on both sides
- Compatible decoding logic
- Backward compatibility consideration


## 3. Encryption Consideration

The new FrameType will automatically:

- Be encrypted post-handshake
- Be bound into AES-GCM AAD
- Be integrity protected

No additional crypto configuration is required.

## 4. Dispatcher Registration

After adding a FrameType, you must:

- Create a handler (if inbound)
- Register it in the dispatcher

See AddingNewHandler.md

## 5. Protocol Stability Rules

FrameTypes are:

- Versioned implicitly
- Contractual
- Part of public protocol behavior

Changing semantics of an existing FrameType is forbidden.

Instead:

- Add a new FrameType.