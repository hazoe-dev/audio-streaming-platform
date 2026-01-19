# 🎵 Audio Streaming Design

This document describes how audio streaming is implemented in the Audio Streaming Platform backend, focusing on **HTTP Range–based streaming**, **security enforcement**, and **clean responsibility separation**.

---

## 📌 Overview

Audio playback is implemented using **HTTP Range Requests**, allowing clients to request specific byte ranges of audio files.

All streaming requests are routed **through the backend**, ensuring:

* Proper **JWT authentication**
* **Premium access enforcement**
* Safe and controlled byte streaming

The backend remains **audio-format agnostic** and streams raw bytes without decoding or transcoding.

---

## 🎯 Design Goals

This streaming design aims to:

* Enforce **authentication & premium access**
* Support **HTTP range-based streaming**
* Clearly separate:

   * Security concerns
   * Streaming logic
   * Storage access

---

## 🧠 Key Concepts

### HTTP Range Requests

Clients request partial content using the `Range` HTTP header:

```http
Range: bytes=1000000-
```

Meaning:

> “Send the audio starting from byte 1,000,000 until the end.”

This enables:

* Progressive playback
* Seeking
* Resume listening
* Efficient bandwidth usage

---

## 🔄 High-level Streaming Flow

```text
Client (Browser / Mobile Player)
    |
    | GET /api/audios/{id}/stream
    | Authorization: Bearer <JWT>
    | Range: bytes=...
    |
    v
Spring Security Filter Chain
    |
    |-- JwtAuthenticationFilter
    |     - Parse JWT
    |     - Set SecurityContext
    |
    v
AudioController
    |
    |-- Validate audio exists
    |-- Validate premium access
    |
    v
AudioService
    |
    |-- Resolve audio_path
    |-- Validate file exists
    |
    v
StreamingService
    |
    |-- Parse Range header
    |-- Stream requested byte range
    |
    v
Storage (File System / Object Storage)
    |
    v
HTTP 206 Partial Content
```

---

## 🔍 Detailed Request Sequence

```text
Client
  |
  |--- GET /api/audios/{id}/stream
  |     Range: bytes=5000000-8000000
  |     Authorization: Bearer <JWT>
  |
  v
JwtAuthenticationFilter
  |
  |-- JWT valid?
  |     NO → 401 Unauthorized
  |     YES → set SecurityContext
  |
  v
AudioController
  |
  |-- Load audio metadata
  |-- is_premium?
  |     |
  |     |-- Not authenticated → 401
  |     |-- Not premium       → 403
  |
  v
AudioService
  |
  |-- Resolve audio_path
  |
  v
StreamingService
  |
  |-- Parse Range header
  |-- Calculate start / end bytes
  |-- Stream partial content
  |
  v
HTTP Response
      Status: 206 Partial Content
      Content-Range: bytes 5000000-8000000/52428800
      Content-Type: audio/mpeg
```

---

## 📤 Response Semantics

### Partial Content Response

```http
HTTP/1.1 206 Partial Content
Accept-Ranges: bytes
Content-Range: bytes 1000000-2000000/5000000
Content-Length: 1000001
Content-Type: audio/mpeg
```

### Full Content (No Range Header)

```http
HTTP/1.1 200 OK
Accept-Ranges: bytes
Content-Length: 5000000
Content-Type: audio/mpeg
```

---

## ⚠️ Invalid Range Handling

If the requested range is invalid:

```http
Range: bytes=999999999-
```

Response:

```http
HTTP/1.1 416 Range Not Satisfiable
Content-Range: bytes */5000000
```

This strictly follows the HTTP specification.

---

## ⏯️ Seek & Resume Listening

### Resume Flow

1. Client stores last playback position in **seconds**
2. Client converts seconds → byte offset
3. Client issues a new Range request:

```http
Range: bytes=XYZ-
```

> The backend does **not** convert time to bytes.
> This responsibility is intentionally kept on the client to avoid format-specific logic.

---

## 🛡️ Security Boundary

### Authorization Before Streaming

* JWT authentication occurs in the **Spring Security filter chain**
* Premium access is validated **before any bytes are streamed**
* Unauthorized requests are rejected early

This prevents:

* Partial data leakage
* Unauthorized bandwidth consumption

---

## 🧱 Responsibility Separation

| Component                 | Responsibility                      |
| ------------------------- | ----------------------------------- |
| `JwtAuthenticationFilter` | Authentication & SecurityContext    |
| `AudioController`         | Request validation & access control |
| `AudioService`            | Metadata & storage path resolution  |
| `StreamingService`        | Range parsing & byte streaming      |

Business rules are fully isolated from low-level I/O operations.

---

## 📦 Storage Abstraction

Streaming logic is independent of storage implementation:

* Local filesystem (current)
* Object storage (e.g. S3) in future iterations

No domain logic changes are required when switching storage backends.

---

## ❌ Error Handling Behavior

| Scenario                 | Response                    |
| ------------------------ | --------------------------- |
| Audio not found          | `404 Not Found`             |
| No JWT for premium audio | `401 Unauthorized`          |
| Non-premium user         | `403 Forbidden`             |
| Invalid Range header     | `416 Range Not Satisfiable` |

👉 This table is **interview gold**.

---

## 🚀 Performance Considerations

* Streaming is read-only
* No database access during byte transfer
* Metadata is resolved once per request
* Streams data in chunks (low memory footprint)
* Supports large audio files and high concurrency

---

## 🧠 Design Trade-offs

### Why stream through backend instead of direct storage access?

> “Streaming through the backend allows us to enforce authentication and premium access consistently, while still supporting efficient HTTP range-based streaming.”

### Why HTTP Range instead of WebSockets?

* Native browser support
* Cache & CDN friendly
* Simpler infrastructure
* Protocol-standard for media delivery

---

## 📎 Summary

Audio streaming is implemented using HTTP range requests.
Each request passes through the security filter chain, where JWT authentication and premium access are enforced.
The backend streams partial audio content from storage and returns `206 Partial Content`, enabling efficient playback and seeking without downloading the entire file.


---

