# Audio Streaming Platform — Component Diagrams

## 1. System Architecture Overview

```mermaid
graph TB
    Client["Client (Web / Mobile)"]

    subgraph Docker["Docker Compose"]
        Nginx["Nginx\n(Reverse Proxy :80)"]
        API["Spring Boot API\n(:8080)"]
        PG[("PostgreSQL 18\n(:5432)")]
        Storage[("Storage Volume\n/audio  /cover")]
    end

    Client -->|HTTP| Nginx
    Nginx -->|"Proxy /api/*"| API
    Nginx -->|"Serve static files"| Storage
    API -->|"JPA / Flyway"| PG
    API -->|"RandomAccessFile"| Storage
```

---

## 2. Module Dependency Map

### 2a. Layer Overview

```mermaid
graph TB
    subgraph Foundation["Foundation Layer"]
        common["common<br/>SecurityConfig · JwtAuthFilter <br/>GlobalExceptionHandler · UserPrincipal"]
    end

    subgraph Feature["Feature Layer"]
        auth["auth<br/>Register / Login / JWT"]
        audio["audio<br/>CRUD · Stream · Mapper"]
        search["search<br/>Full-text search"]
        library["library<br/>User saved audios"]
        progress["progress<br/>Listening position"]
    end

    auth     -->|"uses security & error handling"| common
    audio    -->|"uses security & error handling"| common
    search   -->|"uses security & error handling"| common
    library  -->|"uses security & error handling"| common
    progress -->|"uses security & error handling"| common

    library  -->|"AudioQueryService interface"| audio
    progress -->|"AudioQueryService interface"| audio
    search   -->|"AudioQueryService interface"| audio
```

### 2b. Internal Module Detail

```mermaid
graph TB
    subgraph common["common"]
        direction LR
        SC["SecurityConfig"] --- JF["JwtAuthFilter"]
        JF --- UP["UserPrincipal"]
        UP --- GEH["GlobalExceptionHandler"]
    end

    subgraph auth["auth"]
        direction LR
        AuC["AuthController"] --> AuS["AuthService"]
        AuS --> JWT["JwtProvider"]
        AuS --> RTS["RefreshTokenService"]
    end

    subgraph audio["audio"]
        direction LR
        AdC["AudioController"] --> AdS["AudioService"]
        AdC --> ASt["AudioStreamService"]
        ASt --> RR["RangeResolver"]
        AdS --> AM["AudioMapper"]
        AA["AudioAuthorization"]
    end

    subgraph search["search"]
        direction LR
        SeC["AudioSearchController"] --> SeS["AudioSearchService"]
    end

    subgraph library["library"]
        direction LR
        LC["LibraryController"] --> LS["LibraryService"]
    end

    subgraph progress["progress"]
        direction LR
        PC["ListeningProgressController"] --> PS["ListeningProgressService"]
    end

    auth     -.->|"uses common"| common
    audio    -.->|"uses common"| common
    search   -.->|"uses common"| common
    library  -.->|"uses common"| common
    progress -.->|"uses common"| common

    library  ===>|"AudioQueryService"| audio
    progress ===>|"AudioQueryService"| audio
    search   ===>|"AudioQueryService"| audio
```

---

## 3. Entity Relationship Diagram

```mermaid
erDiagram
    users {
        BIGINT id PK
        VARCHAR email UK
        VARCHAR password_hash
        VARCHAR role
        TIMESTAMPTZ created_at
    }
    audio {
        BIGINT id PK
        VARCHAR title
        TEXT description
        INTEGER duration_seconds
        VARCHAR audio_path
        VARCHAR cover_path
        BOOLEAN is_premium
        BIGINT owner_id FK
        TSVECTOR search_vector
        TIMESTAMPTZ created_at
    }
    refresh_tokens {
        BIGINT id PK
        VARCHAR token UK
        BIGINT user_id FK
        TIMESTAMPTZ expires_at
    }
    library_item {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT audio_id FK
        TIMESTAMPTZ saved_at
    }
    listening_progress {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT audio_id FK
        INTEGER position_seconds
        TIMESTAMPTZ updated_at
    }

    users ||--o{ refresh_tokens : "has"
    users ||--o{ library_item : "saves"
    users ||--o{ listening_progress : "tracks"
    users ||--o{ audio : "owns"
    audio ||--o{ library_item : "in"
    audio ||--o{ listening_progress : "tracked by"
```

---

## 4. Authentication Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant AC as AuthController
    participant AS as AuthService
    participant JP as JwtProvider
    participant RTS as RefreshTokenService
    participant DB as PostgreSQL

    Note over C,DB: Register
    C->>AC: POST /api/auth/register {email, password}
    AC->>AS: save(RegisterRequest)
    AS->>DB: INSERT users (BCrypt hash)
    AS-->>C: 201 {email, message}

    Note over C,DB: Login
    C->>AC: POST /api/auth/login {email, password}
    AC->>AS: authenticate(LoginRequest)
    AS->>DB: SELECT user WHERE email = ?
    AS->>JP: generateAccessToken(userId, role)
    AS->>JP: generateRefreshToken(userId)
    AS->>RTS: rotate(user)
    RTS->>DB: DELETE old tokens, INSERT new refresh_token
    AS-->>C: 200 {accessToken, refreshToken}

    Note over C,DB: Token Refresh
    C->>AC: POST /api/auth/refresh {refreshToken}
    AC->>AS: refreshToken(request)
    AS->>RTS: validate(token)
    RTS->>DB: SELECT refresh_tokens WHERE token = ?
    AS->>RTS: rotate(user)
    AS-->>C: 200 {newAccessToken, newRefreshToken}
```

---

## 5. Request Authorization Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant F as JwtAuthFilter
    participant JP as JwtProvider
    participant Ctrl as Controller
    participant Svc as Service

    C->>F: HTTP Request [Authorization: Bearer <token>]
    F->>JP: getPrincipalFromToken(token)
    alt Valid token
        JP-->>F: UserPrincipal(userId, role)
        F->>F: Set SecurityContext
        F->>Ctrl: Forward request
        Ctrl->>Svc: Execute business logic
        Svc-->>C: 200 Response
    else Invalid / expired token
        JP-->>F: throw UnauthorizedException
        F-->>C: 401 Unauthorized
    end
```

---

## 6. Audio Streaming Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant AA as AudioAuthorization
    participant ASt as AudioStreamService
    participant RR as RangeResolver
    participant FS as FileSystem

    C->>AA: GET /api/audios/{id}/stream [Range: bytes=0-1048575]
    AA->>AA: canStream(audioId, authentication)
    Note right of AA: FREE audio → allow all<br/>PREMIUM audio → require PREMIUM or ADMIN role

    AA->>ASt: stream(id, rangeHeader)
    ASt->>RR: resolve(rangeHeader, fileSize)
    RR-->>ASt: ByteRange(start, end, total)
    ASt->>FS: RandomAccessFile.seek(start)
    FS-->>ASt: byte[] chunk (~1 MB)
    ASt-->>C: 206 Partial Content\nContent-Range: bytes start-end/total\nContent-Type: audio/mpeg
```

---

## 7. Full-Text Search Flow

### 7a. Request Pipeline

```mermaid
sequenceDiagram
    participant C as Client
    participant SC as AudioSearchController
    participant SS as AudioSearchService
    participant R as AudioRepository
    participant DB as PostgreSQL

    C->>SC: GET /api/audios/search?q=mindful focus

    SC->>SS: search("mindful focus", pageable)

    Note over SS: Step 1 — Sanitize: strip special chars to prevent tsquery syntax errors
    Note over SS: Step 2 — Tokenize: split by whitespace → ["mindful", "focus"]
    Note over SS: Step 3 — Prefix flag: append :* → ["mindful:*", "focus:*"]
    Note over SS: Step 4 — OR logic: join with | → "mindful:* | focus:*"

    SS->>R: search("mindful:* | focus:*", pageable)

    R->>DB: SELECT * FROM audio<br/>WHERE search_vector @@ to_tsquery('english', 'mindful:* | focus:*')<br/>ORDER BY ts_rank_cd(search_vector, ...) DESC

    Note over DB: Uses GIN index on search_vector — O(log N), no full table scan

    DB-->>R: Page<Audio> ranked by relevance
    R-->>SS: Page<Audio>
    SS-->>C: 200 Page<AudioListItemDto> (most relevant first)
```

### 7b. FTS Key Benefits vs LIKE

```mermaid
graph TB
    subgraph index["GIN Index on search_vector"]
        I1["O(log N) lookup<br/>vs LIKE full table scan"]
    end

    subgraph pipeline["Keyword Processing Pipeline"]
        P1["'mindful focus'"]
        P2["sanitize — remove special chars"]
        P3["tokenize — split by whitespace"]
        P4["prefix :* — match partial words<br/>'mind' matches 'mindful'"]
        P5["OR join | — any word matches<br/>'mindful:* | focus:*'"]
        P1 --> P2 --> P3 --> P4 --> P5
    end

    subgraph fts["PostgreSQL FTS Features Used"]
        F1["search_vector — GENERATED STORED column<br/>auto-updated on title/description change"]
        F2["to_tsvector('english') — stemming<br/>'running' = 'run' = 'runs'"]
        F3["@@ operator — match tsquery against tsvector"]
        F4["ts_rank_cd() — relevance score<br/>title hit ranks higher than description hit"]
    end

    subgraph result["Result"]
        R1["Most relevant audio appears first<br/>Partial matches included<br/>Consistent speed at any data size"]
    end

    pipeline --> fts --> result
    index --> result
```

---

## 8. API Endpoint Map

```mermaid
graph LR
    subgraph Public["Public (no auth)"]
        E1["POST /api/auth/register"]
        E2["POST /api/auth/login"]
        E3["POST /api/auth/refresh"]
        E4["GET  /api/audios"]
        E5["GET  /api/audios/{id}"]
        E6["GET  /api/audios/search?q="]
    end

    subgraph Auth["Authenticated"]
        E7["GET    /api/library"]
        E8["POST   /api/library/{audioId}"]
        E9["DELETE /api/library/{audioId}"]
        E10["PUT  /api/progress"]
        E11["GET  /api/progress/{audioId}"]
    end

    subgraph Conditional["Conditional (premium check)"]
        E12["GET /api/audios/{id}/stream"]
    end

    style Public fill:#2d6a4f,color:#fff
    style Auth fill:#1d3557,color:#fff
    style Conditional fill:#6d4c41,color:#fff
```

---

## 9. List Audios (Paginated)

```mermaid
sequenceDiagram
    participant C as Client
    participant AC as AudioController
    participant AS as AudioService
    participant AM as AudioMapper
    participant DB as PostgreSQL

    C->>AC: GET /api/audios?page=0&size=20
    AC->>AS: getAudios(pageable)
    AS->>DB: SELECT * FROM audio\nORDER BY created_at DESC\nLIMIT 20 OFFSET 0
    DB-->>AS: Page<Audio>
    AS->>AM: toListItemDto(audio) × N
    AM-->>AS: AudioListItemDto(id, title, durationSeconds, isPremium)
    AS-->>AC: Page<AudioListItemDto>
    AC-->>C: 200 {content, page, size, totalElements}
```

---

## 10. Get Audio Detail

```mermaid
sequenceDiagram
    participant C as Client
    participant AC as AudioController
    participant AS as AudioService
    participant AM as AudioMapper
    participant DB as PostgreSQL

    C->>AC: GET /api/audios/{id}
    AC->>AS: getAudioDetail(id)
    AS->>DB: SELECT * FROM audio WHERE id = ?
    alt Audio found
        DB-->>AS: Audio entity
        AS->>AM: toDetailDto(audio)
        AM-->>AS: AudioDetailDto(id, title, description, durationSeconds, coverUrl, isPremium)
        AS-->>AC: AudioDetailDto
        AC-->>C: 200 AudioDetailDto
    else Not found
        DB-->>AS: empty
        AS-->>AC: throw AudioNotFoundException
        AC-->>C: 404 {code, error, message}
    end
```

---

## 11. Save Audio to Library

```mermaid
sequenceDiagram
    participant C as Client
    participant LC as LibraryController
    participant LS as LibraryService
    participant AQS as AudioQueryService
    participant DB as PostgreSQL

    C->>LC: POST /api/library/{audioId}\n[Authorization: Bearer token]
    LC->>LS: save(userId, audioId)
    LS->>AQS: existsById(audioId)
    AQS->>DB: SELECT 1 FROM audio WHERE id = ?
    alt Audio not found
        AQS-->>LS: false
        LS-->>C: 404 AudioNotFoundException
    else Audio exists
        AQS-->>LS: true
        LS->>DB: INSERT INTO library_item (user_id, audio_id, saved_at)\nON CONFLICT DO NOTHING
        DB-->>LS: ok (idempotent)
        LS-->>LC: void
        LC-->>C: 204 No Content
    end
```

---

## 12. List User Library

```mermaid
sequenceDiagram
    participant C as Client
    participant LC as LibraryController
    participant LS as LibraryService
    participant AQS as AudioQueryService
    participant DB as PostgreSQL

    C->>LC: GET /api/library [Authorization: Bearer token]
    LC->>LS: list(userId)

    LS->>DB: Query 1 — SELECT * FROM library_item WHERE user_id = ?
    DB-->>LS: List<LibraryItem> (N items)

    Note over LS: Collect all audioIds from items into a List<Long>
    Note over LS,AQS: Avoid N+1 — do NOT call findById() per item

    LS->>AQS: getSummaryList(audioIds)
    AQS->>DB: Query 2 — SELECT * FROM audio WHERE id IN (id1, id2, ..., idN)
    DB-->>AQS: List<Audio> (all at once)

    Note over LS,DB: Total: 2 queries regardless of N items<br/>(naive loop would cost 1 + N queries)

    AQS-->>LS: List<AudioListItemDto>
    LS->>LS: map to LibraryItemDto
    LS-->>LC: List<LibraryItemDto>
    LC-->>C: 200 [{id, title, durationSeconds, isPremium}, ...]
```

---

## 13. Remove Audio from Library

```mermaid
sequenceDiagram
    participant C as Client
    participant LC as LibraryController
    participant LS as LibraryService
    participant DB as PostgreSQL

    C->>LC: DELETE /api/library/{audioId}\n[Authorization: Bearer token]
    LC->>LS: delete(userId, audioId)
    LS->>DB: DELETE FROM library_item\nWHERE user_id = ? AND audio_id = ?
    DB-->>LS: ok
    LS-->>LC: void
    LC-->>C: 204 No Content
```

---

## 14. Save / Update Listening Progress

```mermaid
sequenceDiagram
    participant C as Client
    participant PC as ListeningProgressController
    participant PS as ListeningProgressService
    participant AQS as AudioQueryService
    participant DB as PostgreSQL

    C->>PC: PUT /api/progress\n{audioId, positionSeconds}\n[Authorization: Bearer token]
    PC->>PS: saveProgress(userId, audioId, positionSeconds)
    PS->>AQS: getDetailsById(audioId)
    AQS->>DB: SELECT * FROM audio WHERE id = ?
    alt Audio not found
        AQS-->>PS: empty
        PS-->>C: 404 AudioNotFoundException
    else Position out of range
        PS->>PS: positionSeconds > durationSeconds?
        PS-->>C: 400 InvalidProgressPositionException
    else Valid
        PS->>DB: SELECT * FROM listening_progress\nWHERE user_id = ? AND audio_id = ?
        alt First time
            DB-->>PS: empty
            PS->>DB: INSERT INTO listening_progress\n(user_id, audio_id, position_seconds, updated_at)
        else Existing record
            DB-->>PS: ListeningProgress entity
            PS->>DB: UPDATE listening_progress\nSET position_seconds = ?, updated_at = NOW()\nWHERE id = ?
        end
        PS-->>PC: void
        PC-->>C: 204 No Content
    end
```

---

## 15. Get Listening Progress

```mermaid
sequenceDiagram
    participant C as Client
    participant PC as ListeningProgressController
    participant PS as ListeningProgressService
    participant AQS as AudioQueryService
    participant DB as PostgreSQL

    C->>PC: GET /api/progress/{audioId}\n[Authorization: Bearer token]
    PC->>PS: getProgress(userId, audioId)
    PS->>AQS: existsById(audioId)
    AQS->>DB: SELECT 1 FROM audio WHERE id = ?
    alt Audio not found
        AQS-->>PS: false
        PS-->>C: 404 AudioNotFoundException
    else Audio exists
        AQS-->>PS: true
        PS->>DB: SELECT * FROM listening_progress\nWHERE user_id = ? AND audio_id = ?
        alt Progress found
            DB-->>PS: ListeningProgress
            PS-->>PC: positionSeconds (actual value)
        else No progress yet
            DB-->>PS: empty
            PS-->>PC: positionSeconds = 0 (default)
        end
        PC-->>C: 200 {audioId, positionSeconds}
    end
```

---

## 16. Deployment Architecture

```mermaid
graph TB
    subgraph Host["Host Machine"]
        subgraph Compose["docker-compose.yml"]
            N["nginx\nImage: nginx:alpine\nPort: 80→80\nVolume: ./storage:/storage"]
            A["api\nImage: audiostreaming\nPort: 8080 (internal)\nEnv: DB_*, JWT_*, APP_*"]
            P["postgres\nImage: postgres:18\nPort: 5432 (internal)\nVolume: pgdata"]
        end
        V1[("pgdata\n(named volume)")]
        V2[("./storage\n(bind mount)")]
    end

    N -->|"proxy_pass"| A
    N -->|"alias /storage"| V2
    A -->|"JDBC"| P
    A -->|"file I/O"| V2
    P --- V1
```
