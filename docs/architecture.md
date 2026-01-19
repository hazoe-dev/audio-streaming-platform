# 🏗️ System Architecture

This document describes the architectural design of the Audio Streaming Platform backend, including structural decisions, module boundaries, and scalability considerations.

---

## 📌 Architectural Style

### Modular Monolith

The system is implemented as a **Modular Monolith**:

* Single deployable unit
* Shared database
* Clear internal module boundaries
* Strong separation of concerns

This approach balances **simplicity**, **transactional consistency**, and **development velocity**, while preserving a clear path to future microservice extraction.

---

## 🎯 Why Modular Monolith?

### Chosen Because

* Avoids premature distributed complexity
* Enables atomic transactions
* Simplifies local development and debugging
* Keeps infrastructure costs low
* Mirrors real-world backend evolution

### Compared to Microservices

| Aspect               | Modular Monolith | Microservices     |
| -------------------- | ---------------- | ----------------- |
| Deployment           | Single unit      | Multiple services |
| Transactions         | Local            | Distributed       |
| Operational overhead | Low              | High              |
| Development speed    | Fast             | Slower            |
| Failure isolation    | Process-level    | Service-level     |

> Microservices are a future consideration once scale and organizational complexity demand it.

---

## 📦 Module Boundaries (Bounded Contexts)

Each top-level package represents a **bounded context** with its own responsibility.

| Module     | Responsibility                                 |
| ---------- | ---------------------------------------------- |
| `auth`     | Authentication, authorization, token lifecycle |
| `audio`    | Audio metadata and access rules                |
| `library`  | User-owned audio                               |
| `progress` | Resume listening state                         |
| `search`   | Full-text search                               |
| `common`   | Cross-cutting concerns                         |
| `config`   | Application configuration                      |

Modules communicate via **service interfaces**, not repositories or entities.

---

## 🧱 Layered Structure (Inside Each Module)

Each module follows a consistent internal structure:

```text
controller → service → domain → repository
```

### Layer Responsibilities

| Layer      | Responsibility                 |
| ---------- | ------------------------------ |
| Controller | HTTP, validation, DTO mapping  |
| Service    | Business logic & authorization |
| Domain     | Core entities & state          |
| Repository | Data access                    |

This enforces:

* Clear responsibility separation
* Testable business logic
* Framework isolation

---

## 🧠 Domain Model Design

### Anemic Domain (By Design)

* Domain entities primarily represent persistence state
* Business logic lives in services
* Reduces complexity at early stages

> The domain model can evolve toward richer behavior as business rules grow.

---

### Domain & Persistence Coupling

Domain entities are implemented as **JPA entities**.

**Trade-off:**

* Reduced purity
* Lower complexity
* Fewer duplicate models

Framework-specific concerns are kept out of domain logic.

---

## 🔐 Security Architecture

### Authentication

* JWT-based authentication
* Stateless access tokens
* Stateful refresh tokens

Authentication is resolved **once** at the filter layer.

---

### Authorization

Authorization is enforced at:

* Controller boundaries (`@PreAuthorize`)
* Service level (business rules)

> Security decisions are validated before streaming or other I/O operations.

---

## 🎵 Streaming Architecture

### Responsibility Split

| Component        | Responsibility       |
| ---------------- | -------------------- |
| Controller       | HTTP handling        |
| AudioService     | Metadata & access    |
| StreamingService | Byte-range streaming |

Streaming is:

* Stateless
* Read-only
* Isolated from business logic

---

## 🔍 Search Architecture

Search is implemented using:

* PostgreSQL full-text search
* `tsvector` columns
* GIN indexes

This avoids introducing Elasticsearch prematurely while meeting current requirements.

---

## 🗄️ Data Architecture

* Single PostgreSQL database
* ACID transactions
* Explicit constraints & indexes
* JPA-managed schema

> The database acts as the system of record.

---

## ⚙️ Configuration & Cross-Cutting Concerns

### Common Module

Contains:

* Security configuration
* Exception handling
* API response models

This avoids duplication across modules.

---

## 🚀 Scalability & Evolution Strategy

### Planned Evolution

The architecture supports gradual evolution:

1. Introduce caching for metadata
2. Externalize audio storage (S3/CDN)
3. Extract streaming into a separate service
4. Event-driven progress updates
5. Move toward microservices if required

---

## 🔄 Transaction Strategy

* Transactions are local and ACID
* No distributed transactions
* Streaming operations are non-transactional

This ensures consistency without distributed complexity.

---

## 🧠 Trade-offs & Constraints

| Decision           | Trade-off                         |
| ------------------ | --------------------------------- |
| Modular monolith   | Less isolation than microservices |
| Anemic domain      | Less expressive domain            |
| PostgreSQL search  | Limited advanced search features  |
| JPA-managed schema | Less DB-specific optimization     |

All trade-offs are intentional and documented.

---

## 📎 Notes for Interview Discussion

Strong talking points:

* Why modular monolith first
* Security resolved once per request
* Authorization before I/O
* Evolution path to microservices
* Avoiding premature optimization

---

## 🔗 Related Documents

* [Authentication & Authorization](authentication.md)
* [Audio Streaming Design](streaming.md)
* [Database Schema](database.md)

---
