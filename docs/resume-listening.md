
# 🎧 Step 6 – Resume Listening

## 🎯 Goal

Allow authenticated users to:

* Resume audio playback from the **last listened position**
* Persist listening progress **per user & per audio**
* Support seek / pause / resume **without re-downloading audio**

> Resume listening is **user-scoped**, **audio-scoped**, and independent of the library.


## 1️⃣ Scope Definition

### In scope ✅

* Save listening progress
* Fetch last listened position
* Update progress during playback

### Out of scope ❌

* Playback history
* Analytics
* Cross-device conflict resolution (future)
* Completion tracking (future)


## 2️⃣ Domain Concept

### 🎧 Resume Listening = State, not Content

Resume listening represents **ephemeral behavioral state**, not core domain data.

Relationship:

```text
User 1 ─── * ListeningProgress * ─── 1 Audio
````

Constraints & rules:

* One progress record per `(user, audio)`
* Progress is **owned by the user**
* Audio existence is required
* Progress is **overwritten**, not accumulated
* No dependency on user library membership


## 3️⃣ Database Design

### Table: `listening_progress`

```sql
CREATE TABLE listening_progress (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    audio_id BIGINT NOT NULL,
    position_seconds INT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT uq_progress_user_audio UNIQUE (user_id, audio_id),
    CONSTRAINT fk_progress_audio FOREIGN KEY (audio_id) REFERENCES audio(id)
);
```

### Design Notes

* **One progress record per (user, audio)**, enforced via a database unique constraint
* Uses a surrogate key for ORM simplicity
* `user_id` is stored as a **scalar value** to avoid coupling with the User aggregate
* `updated_at` is managed by the database to ensure consistency
* Resume listening data is treated as **behavioral state**, not user identity

### Why a surrogate key?

* Simpler ORM mapping
* Easier future extensions (completion flag, percentage, device info)
* Consistent with other state-based tables (e.g. `library_item`)


## 4️⃣ API Design

### 4.1 Get Resume Position

```
GET /api/progress/{audioId}
```

**Auth:** Required

**Response – 200 OK**

```json
{
  "audioId": 1,
  "positionSeconds": 742
}
```

> If no record exists, `positionSeconds = 0`


### 4.2 Update Resume Position

```
PUT /api/progress/{audioId}
```

**Request Body**

```json
{
  "positionSeconds": 742
}
```

**Rules**

* Idempotent
* Overwrites existing progress
* User identity is always derived from JWT

**Response – 204 No Content**


## 5️⃣ Streaming Integration (Concept)

Resume listening is **loosely coupled** with streaming.

Flow:

1. Client streams audio using HTTP Range requests
2. Client periodically sends progress updates
3. Backend persists the last known position

> Streaming **never** modifies progress directly
> Progress updates are explicit client actions

This separation keeps responsibilities clean and decoupled.


## 6️⃣ Service Design

### ListeningProgressService (Concrete)

Responsibilities:

* Fetch last position
* Upsert progress
* Enforce user ownership
* Validate bounds

```java
@Service
@RequiredArgsConstructor
public class ListeningProgressService {

    public int getProgress(Long userId, Long audioId) { ... }

    public void updateProgress(Long userId, Long audioId, int positionSeconds) { ... }
}
```

> No interface is used — there is a single stable implementation.
> This avoids premature abstraction.


## 7️⃣ Controller Design

```java
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ListeningProgressController {

    private final ListeningProgressService service;

    @GetMapping("/{audioId}")
    public ResponseEntity<ProgressDto> getProgress(
            @PathVariable Long audioId,
            Authentication auth) {

        Long userId = AuthUtils.getUserId(auth);
        int position = service.getProgress(userId, audioId);
        return ResponseEntity.ok(new ProgressDto(audioId, position));
    }

    @PutMapping("/{audioId}")
    public ResponseEntity<Void> updateProgress(
            @PathVariable Long audioId,
            @RequestBody UpdateProgressRequest request,
            Authentication auth) {

        Long userId = AuthUtils.getUserId(auth);
        service.updateProgress(userId, audioId, request.positionSeconds());
        return ResponseEntity.noContent().build();
    }
}
```


## 8️⃣ Validation Rules

* `positionSeconds >= 0`
* `positionSeconds <= audio.durationSeconds`
* Audio must exist
* User must be authenticated
* Users cannot access other users’ progress


## 9️⃣ Tests to Write

### Service

* create new progress
* update existing progress
* overwrite position
* invalid audio ID
* out-of-range position

### Controller (MockMvc)

* authenticated success
* unauthenticated → 401
* invalid payload → 400

