💬 Modular Secure Chat System (Java)

A modular, extensible, and security-oriented chat system built in pure Java.

This project began as a university assignment and evolved into a deliberate architectural exercise focused on applying the Open/Closed Principle (OCP) consistently and rigorously.

Rather than building a simple chat client, the goal became:

Design a system that can grow without requiring modification of existing components.

This repository reflects that exploration.

🎯 Project Motivation

The core objective of this project was not feature complexity —
it was architectural discipline.

From the beginning, one principle guided all design decisions:

Open for extension, closed for modification.

Patterns, layering, abstractions, and protocol boundaries were introduced only when they helped preserve that principle.

As the project expanded, it became a sandbox for exploring:

Protocol design
Layered architecture
Explicit security boundaries
Thread isolation
Strategy-based extensibility
Clean documentation practices

It is both a functional system and a learning-driven engineering project.

🏗 Architectural Overview

The client follows a layered structure with strict separation of concerns:

User Input
   ↓
Command System (Strategy-based)
   ↓
Message Gateway (Abstraction Boundary)
   ↓
Framed Connection Layer
   ↓
Secure Frame Protocol
   ↓
TCP Transport

Architectural Characteristics:

No raw socket access outside the connection layer
No encryption logic outside the crypto boundary
No protocol interpretation inside runtime orchestration
Explicit frame-based communication model
Interface-driven design across all subsystems

The system favors composition over inheritance and clearly defined extension points.

📦 Custom Frame-Based Protocol

All communication is performed via a custom length-prefixed binary protocol.

Field	Type	Description:

length	4 bytes	Total frame size (excluding this field)
type	4 bytes	FrameType identifier
payload	variable	Structured data
Properties
Strongly typed protocol via FrameType enum
Deterministic parsing
No raw text over TCP
Explicit payload encoding order
Stable protocol contracts

Protocol design is treated as a first-class architectural concern.

🔐 Security Model

After handshake, all communication is encrypted.

AES-GCM for authenticated encryption
Frame type bound via Additional Authenticated Data (AAD)
Clear separation between plaintext handshake and encrypted session traffic
Explicit cryptographic boundary

Security is intentionally modeled as a layer, not scattered throughout the system.

🧠 Command System (OCP-Centric Design)

The command system is strategy-based.

Adding a new command requires:

Creating a new class
Registering it
No modification of existing commands

Extension path:

New Command → Registration → Done

Commands:

Represent user intent
Translate input into protocol frames
Have no networking or encryption logic
Remain isolated from runtime orchestration

This structure ensures safe extension without regression.

🧵 Threading Model

The client uses explicit thread separation for:

Frame reading (network I/O)
Command processing
File streaming

This prevents:

Blocking I/O from affecting user interaction
File operations from interfering with frame dispatch
Tight coupling between runtime and transport

Concurrency decisions were made deliberately rather than incidentally.

📚 Documentation as Architecture

The repository includes structured technical documentation:

Architecture overview
Protocol specification
Security model
Component boundaries
Extension guides
Future improvement roadmap

Documentation is treated as part of the engineering effort, not an afterthought.

🧪 Concepts Demonstrated

This project explores and applies:

SOLID principles (with strong emphasis on OCP)
Strategy pattern
Dispatcher pattern
Layered architecture
Explicit protocol modeling
Thread isolation
Separation of transport and application layers
Cryptographic boundary design
Extension-oriented system design
📈 Learning Perspective

This project represents a snapshot of my current understanding of:

Java backend architecture
Protocol-driven systems
Clean extensible design

There are certainly areas that can be improved — including testing depth, performance profiling, and scalability modeling — and expanding those areas is part of my ongoing learning goals.

I view this repository not as a finished product, but as a foundation to iterate on and refine as my skills grow.

🔮 Planned Improvements
Improved automated testing coverage
Protocol versioning strategy
Server scalability modeling
Plugin-style command discovery
Performance benchmarking
Improved dependency injection structure

The architecture is intentionally designed to support these extensions without structural refactoring.

👨‍💻 About Me

This project began as academic coursework but evolved into a personal exploration of software architecture and disciplined design in Java.

I am actively seeking opportunities to deepen my knowledge, contribute to real-world systems, and continue improving as an engineer.

Feedback, discussion, and critique are always welcome.
