# User Library

This document describes the **User Library** feature of the Audio Streaming Platform at a **design and API level**.

It intentionally focuses on **scope, domain modeling, API contracts, security rules, and database design**, while leaving **implementation details** to the source code.

## 🎯 Goal

Enable authenticated users to:

* Save audio to their personal library
* Remove audio from their library
* View their own saved items

All library data is **user‑scoped** and **private by default**.

## 1. Scope Definition

### In scope ✅

* Save audio to library
* Remove audio from library
* List the authenticated user’s library

### Out of scope ❌

* Resume listening
* Playlists or folders
* Sharing libraries
* Admin or moderation features

## 2. Domain Model

### Nature of the Relationship

User Library represents a **many‑to‑many** relationship between `User` and `Audio` **with ownership and constraints**.

This is **not** a simple join table:

* A user can save the same audio only once
* The relationship has meaning ("saved by user")
* The relationship may evolve (timestamps, tags, ordering)

For these reasons, the relationship is modeled as a **dedicated entity**.

## 3. Database Design

### Table: `library_item`

* id (PK)
* user_id (FK → users)
* audio_id (FK → audio)
* created_at
* UNIQUE(user_id, audio_id)

### Design Rationale

* `UNIQUE (user_id, audio_id)` prevents duplicate saves
* Surrogate `id` keeps ORM mapping simple and flexible
* Future metadata can be added without schema redesign

## 4. API Design

### 4.1 Save audio to library

```
POST /api/library/{audioId}
Authorization: Bearer <JWT>
```

**Behavior**

* Saves the audio if not already saved
* _**Idempotent**_: repeated calls succeed

**Response**

* `204 No Content`

### 4.2 Remove audio from library

```
DELETE /api/library/{audioId}
Authorization: Bearer <JWT>
```

**Behavior**

* Removes the audio if present
* No error if the audio was not saved

**Response**

* `204 No Content`


### 4.3 List user library

```
GET /api/library
Authorization: Bearer <JWT>
```

**Response – 200 OK**

```json
{
  "items": [
    {
      "id": 1,
      "title": "Mindful Focus",
      "durationSeconds": 1800,
      "isPremium": true
    }
  ]
}
```

Only the _authenticated user’s data_ is returned.

## 5. Security Rules

| Endpoint                      | Rule                    |
| ----------------------------- | ----------------------- |
| POST /api/library             | Authentication required |
| DELETE /api/library           | Authentication required |
| GET /api/library              | Authentication required |
| Access another user’s library | ❌ Impossible            |

### Important Constraint

* `userId` is **never** accepted from the request
* `userId` is always derived from the _JWT principal_

This prevents horizontal privilege escalation.

## 6. Idempotency Guarantees

The Library API is designed to be **safe and idempotent**:

* Saving the same audio multiple times does not create duplicates
* Removing a non‑existent entry does not fail

This simplifies frontend logic and retry handling.

## 7. Package Ownership (High‑level)

```text
library
├─ controller
├─ service
├─ repository
├─ domain
└─ dto
```

Implementation details are intentionally omitted here.

Refer to source code for:

* Entity mappings
* Repository queries
* Service logic
* Controller behavior


## 8. Testing Strategy (Overview)

The following scenarios are covered at controller/service level:

* Anonymous access → `401 Unauthorized`
* Save audio successfully
* Save audio twice → still `204`
* List library returns only user‑owned items
* Remove audio successfully

Database behavior is validated indirectly via integration tests.


## 9. Design Summary

* Library is **user‑scoped** and private
* Relationship modeled as a first‑class entity
* API is idempotent and secure by default
* No user identifiers exposed in requests
* Designed for future extensibility

This approach mirrors **production‑grade backend systems** while remaining simple and maintainable.
