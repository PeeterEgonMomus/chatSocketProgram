#  ➕ Adding a New Command
## 1. Philosophy

The command system is designed around the Open/Closed Principle (OCP).

Adding a new command must NOT require:

Modifying existing commands
Modifying the command processor core logic
Modifying the connection layer

Commands are extensions, not modifications.

---

## 2. Step-by-Step Guide
   Step 1 — Create a New Command Class

Implement the CommandStrategy interface used by the system.

Example structure:

```java
public class WhisperCommand implements CommandStrategy {

    private final ClientMessageGateway gateway;

    public WhisperCommand(ClientMessageGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public void execute(String input) throws Exception {
        // parse arguments
        // validate input
        // create Frame
        // send through gateway
    }

    @Override
    public boolean supports(String input) {
        return input.startsWith("/whisper ");
    }
}
```


The command should:
- Parse arguments 
- Validate input 
- Create protocol frames 
- Send frames via the gateway

The command must NOT:

- Access sockets directly
- Perform encryption
- Manipulate runtime state


---

Step 2 — Register the Command

Register the new command inside the CommandRegistryBuilder.

Example:

```
strategies.add(new WhisperCommand(gateway));
```
No existing command should be modified.

## 3. Design Rules

A command:

- Represents user intent
- Translates intent to protocol frames
- Has a single responsibility

A command does not:

- Interpret incoming frames
- Handle threading
- Perform business logic beyond command scope


## 4. Architectural Guarantee

If adding a command requires modifying existing command classes,
the architecture has been violated.

The correct extension path is:

New class → Registration → Done

## 5. Why This Matters

This design allows:

- Feature growth without regression
- Plugin-style expansion
- Clean testability
- Clear separation of responsibilities

The command system is intentionally built to scale by extension.