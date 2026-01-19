# 🎵 Audio Streaming Design

This document describes how audio streaming is implemented in the Audio Streaming Platform backend, with a focus on **HTTP Range–based streaming**, security boundaries, and performance considerations.

---

## 📌 Overview

Audio playback is implemented using **HTTP Range Requests**, allowing clients to request specific byte ranges of audio files.

This design enables:

* Progressive playback
* Seeking within audio
* Resume listening
* Efficient bandwidth usage

The backend remains **format-agnostic** and streams raw bytes without decoding audio data.

---

## 🧠 Key Concepts

### HTTP Range Requests

Clients request partial content using the `Range` HTTP header:

```http
Range: bytes=1000000-
```

Meaning:

> “Send the audio starting from byte 1,000,000 until the end.”

The server responds with **partial content** instead of the full file.

---

## 🔄 Streaming Request Flow

```text
Client
  |
  | 1. GET /api/audios/{id}/stream
  |    Authorization: Bearer <ACCESS_TOKEN>
  |    Range: bytes=...
  |
  v
AudioController
  |
  | 2. Resolve audio metadata
  | 3. Validate access (FREE vs PREMIUM)
  |
  v
StreamingService
  |
  | 4. Parse Range header
  | 5. Validate byte range
  | 6. Stream requested bytes
  |
  v
Client
```

---

## 📤 Response Semantics

### Successful Partial Content Response

```http
HTTP/1.1 206 Partial Content
Accept-Ranges: bytes
Content-Range: bytes 1000000-2000000/5000000
Content-Length: 1000001
Content-Type: audio/mpeg
```

### Full Content (No Range Header)

If no `Range` header is provided:

```http
HTTP/1.1 200 OK
Accept-Ranges: bytes
Content-Length: 5000000
Content-Type: audio/mpeg
```

---

## ⚠️ Invalid Range Handling

If the client requests an invalid byte range:

```http
Range: bytes=999999999-
```

Server responds:

```http
HTTP/1.1 416 Range Not Satisfiable
Content-Range: bytes */5000000
```

This behavior follows the HTTP specification and prevents undefined behavior.

---

## ⏯️ Seek & Resume Listening

### Resume Flow

1. Client stores last playback position in **seconds**
2. On resume:

    * Client converts seconds → byte offset
    * Sends new `Range` request
3. Server streams from requested offset

```http
Range: bytes=XYZ-
```

> The backend does not convert time to bytes.
> This responsibility is intentionally kept on the client.

---

## 🛡️ Security Boundary

### Authorization Before Streaming

Access validation occurs **before any bytes are streamed**:

* FREE users cannot access premium audio
* Unauthorized requests are rejected early

This prevents:

* Partial data leakage
* Unauthorized bandwidth usage

---

## 🧱 Responsibility Separation

| Component          | Responsibility                 |
| ------------------ | ------------------------------ |
| `AudioController`  | HTTP request handling          |
| `AudioService`     | Metadata & access validation   |
| `StreamingService` | Range parsing & byte streaming |

Business rules are isolated from I/O operations.

---

## 📦 Streaming Implementation Strategy

### Storage

Audio files are accessed via:

* Local filesystem (development)
* Object storage (e.g. S3) in future iterations

Streaming logic is abstracted from storage backend.

---

### I/O Model

* Uses **streaming responses**
* Avoids loading entire files into memory
* Streams byte ranges incrementally

This ensures:

* Low memory footprint
* High concurrency support

---

## 🚀 Performance Considerations

* Streaming is **read-only**
* No database access during byte transfer
* Metadata lookup happens once per request
* Supports large audio files efficiently

---

## ⚠️ Common Pitfalls (Avoided)

| Pitfall                            | Mitigation       |
| ---------------------------------- | ---------------- |
| Loading full file into memory      | Stream in chunks |
| Authorizing after streaming starts | Authorize first  |
| Ignoring invalid ranges            | Return 416       |
| Tying streaming to audio format    | Stream raw bytes |

---

## 🧠 Design Trade-offs

### Why HTTP Range instead of WebSockets?

* HTTP is cache-friendly
* Works with browsers natively
* Simpler infrastructure
* Better CDN compatibility

---

### Why not server-side time-based streaming?

* Audio formats vary
* Bitrate can be variable
* Byte-based streaming is protocol-standard

---

## 🔍 Observability (Future)

Planned improvements:

* Streaming metrics (bytes sent, duration)
* Slow-client detection
* Bandwidth throttling

---

## 📎 Notes for Interview Discussion

Key talking points:

* `206 Partial Content`
* Byte-range semantics
* Resume via client-side conversion
* Security before streaming
* Separation of I/O and business logic

---

## 🔗 Related Documents

* [Authentication & Authorization](authentication.md)
* [Architecture Overview](architecture.md)
* [Database Schema](database.md)

---
