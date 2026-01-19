# 🔐 Authentication & Authorization Design

This document describes the **authentication and authorization model** used in the Audio Streaming Platform backend.

The design prioritizes:

* Security correctness
* Clear responsibility boundaries
* Scalability
* Interview-ready clarity

---

## 📌 Overview

The system uses a **JWT-based authentication model** with a hybrid approach:

* **Access Tokens**

    * Stateless
    * Short-lived
    * Used for request authentication

* **Refresh Tokens**

    * Stateful (persisted in database)
    * Long-lived
    * Rotated on each use

This approach balances **performance** (stateless access tokens) with **security** (revocable refresh tokens).

---

## 🧠 Core Concepts

### Token Types

Two types of JWTs are used:

| Token Type    | Purpose                   | Storage       |
| ------------- | ------------------------- | ------------- |
| Access Token  | Authenticate API requests | Client memory |
| Refresh Token | Issue new access tokens   | Database      |

Token type is explicitly stored using the `typ` claim to prevent misuse.

---

## 🔑 JWT Claims Design

### Access Token Payload

```json
{
  "sub": "42",
  "role": "PREMIUM",
  "type": "ACCESS",
  "issuer": "audiostreaming",
  "iat": 1690000000,
  "exp": 1690003600
}
```

**Notes:**

* `sub` → user ID (immutable identifier)
* `role` → authorization without DB lookup
* `type` → enforced at filter level
* `issuer` → issuer validation
* No sensitive data is stored in tokens

---

### Refresh Token Payload

```json
{
  "sub": "42",
  "type": "REFRESH",
  "issuer": "audiostreaming",
  "iat": 1690000000,
  "exp": 1690007200
}
```

**Differences from access token:**

* No role claim
* Longer expiration
* Accepted only by refresh endpoint

---

## 🔐 Authentication Flow (Login)

```text
Client
  |
  | 1. POST /api/auth/login
  |    { email, password }
  |
  v
AuthController
  |
  | 2. Validate credentials
  |
  v
AuthService
  |
  | 3. Generate ACCESS token (short-lived)
  | 4. Generate REFRESH token (long-lived)
  | 5. Persist refresh token
  |
  v
Client
```

### Result

Client receives:

* `accessToken`
* `refreshToken`

---

## 🔁 Refresh Token Flow (Rotation)

Refresh tokens are **rotated on every use**.

```text
Client
  |
  | Access token expired
  |
  | 1. POST /api/auth/refresh
  |    { refreshToken }
  |
  v
AuthController
  |
  | 2. Validate token signature & type
  | 3. Lookup refresh token in DB
  |
  v
AuthService
  |
  | 4. Delete old refresh token
  | 5. Issue new refresh token
  | 6. Generate new access token
  |
  v
Client
```

### Why rotation?

* Prevents replay attacks
* Enables token revocation
* Detects suspicious reuse

---

## 🔎 Request Authentication Lifecycle

### Incoming Request

```text
[HTTP REQUEST]
    |
    | Authorization: Bearer <ACCESS_TOKEN>
    |
    v
JwtAuthenticationFilter
```

### Filter Logic

```text
JwtAuthenticationFilter
    |
    |-- Token missing?
    |     → Continue as anonymous
    |
    |-- Token invalid / expired?
    |     → Clear context → 401
    |
    |-- Token valid
    |     → Validate issuer & type
    |     → Extract userId + role
    |     → Create UserPrincipal
    |     → Set SecurityContext
```

### Authorization Phase

```text
SecurityFilterChain
    |
    |-- Role allowed?
    |     → YES → Controller
    |     → NO  → 403 Forbidden
```

---

## 🛡️ Authorization Model

### Role-Based Access Control (RBAC)

| Role    | Permissions            |
| ------- | ---------------------- |
| FREE    | Access free audio      |
| PREMIUM | Access premium audio   |
| ADMIN   | Administrative actions |

Authorization is enforced at:

* Controller level (`@PreAuthorize`)
* Service level (business rules)

> Business authorization is validated **before streaming begins** to prevent partial data leakage.

---

## 🗄️ Refresh Token Persistence

Refresh tokens are stored in the database to enable:

* Revocation
* Rotation
* Multi-device support

### Schema (simplified)

```text
refresh_tokens
- id
- token (unique)
- user_id
- expires_at
- created_at
```

Multiple refresh tokens per user are allowed to support multiple devices.

---

## ⚠️ Failure Scenarios & Responses

| Scenario                       | HTTP Status               |
| ------------------------------ | ------------------------- |
| Missing access token           | 401 Unauthorized          |
| Invalid / expired access token | 401 Unauthorized          |
| Valid token, insufficient role | 403 Forbidden             |
| Invalid refresh token          | 401 Unauthorized          |
| Reused refresh token           | 401 Unauthorized + revoke |

---

## 🔐 Security Design Decisions

| Decision                    | Reason                  |
| --------------------------- | ----------------------- |
| Stateless access tokens     | Fast authentication     |
| Stateful refresh tokens     | Revocation & audit      |
| Short-lived access tokens   | Reduced impact of leaks |
| Refresh token rotation      | Replay protection       |
| Token type enforcement      | Prevent token misuse    |
| Role stored in access token | Avoid DB lookup         |

---

## 🧠 Design Trade-offs

### Why not fully stateless JWTs?

* Cannot revoke tokens
* No reuse detection
* Poor security for long-lived sessions

### Why not fully stateful sessions?

* DB hit on every request
* Harder to scale horizontally

👉 Hybrid model provides the best balance.

---

## 📎 Notes for Interview Discussion

Key talking points:
* Token type separation (`ACCESS` vs `REFRESH`)
* Rotation strategy
* Stateless vs stateful trade-offs
* Authorization before streaming
* Multi-device refresh token support

---

## 🔗 Related Documents

* [Architecture Overview](architecture.md)
* [Audio Streaming Design](streaming.md)
* [Database Schema](database.md)

---
