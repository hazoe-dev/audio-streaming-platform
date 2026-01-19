# API Specification

This document defines the HTTP API contract for the **Audio Streaming Platform** backend.

The API is:

* RESTful
* JSON-based
* Secured using JWT Bearer authentication
* Versioned at the URL level

All endpoints are prefixed with:

```
/api
```


## 1. API Conventions

### 1.1 Request Format

* JSON request bodies
* UTF-8 encoding
* `Content-Type: application/json`

### 1.2 Response Format

Successful responses return:

* HTTP `2xx`
* JSON payload (unless streaming)

Error responses follow a common structure:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed"
}
```


### 1.3 Authentication

Most endpoints require authentication via JWT:

```
Authorization: Bearer <access_token>
```

* Only **ACCESS** tokens are accepted
* Expired or invalid tokens result in `401`
* Insufficient permissions result in `403`

Detailed authentication flows are documented in
➡️ [`docs/authentication.md`](authentication.md)


## 2. Authentication API

### 2.1 Register

```
POST /api/auth/register
```

**Request**

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response – 201 Created**

```json
{
    "email": "user@gmail.com",
    "message": "User registered successfully"
}
```

**Errors**

| Status | Reason               |
| ------ | -------------------- |
| 400    | Validation error     |
| 409    | Email already exists |


### 2.2 Login

```
POST /api/auth/login
```

**Request**

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response – 200 OK**

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
}
```


### 2.3 Refresh Token

```
POST /api/auth/refresh
```

**Request**

```json
{
  "refreshToken": "eyJhbGciOi..."
}
```

**Response – 200 OK**

```json
{
  "accessToken": "newAccessToken",
  "refreshToken": "newRefreshToken",
}
```

**Errors**

| Status | Reason                           |
| ------ | -------------------------------- |
| 401    | Invalid or expired refresh token |


## 3. Audio API

### 3.1 List Audio Catalog

```
GET /api/audios
```

**Query Parameters**

| Name | Type | Description |
| ---- | ---- | ----------- |
| page | int  | Page number |
| size | int  | Page size   |

**Response – 200 OK**

```json
{
  "items": [
    {
      "id": 1,
      "title": "Mindful Focus",
      "durationSeconds": 1800,
      "isPremium": false
    }
  ],
  "total": 20
}
```
This endpoint returns metadata only and does not expose audio paths or URLs.


### 3.2 Get Audio Details

```
GET /api/audios/{id}
```

**Response – 200 OK**

```json
{
  "id": 1,
  "title": "Mindful Focus",
  "description": "Guided meditation",
  "durationSeconds": 1800,
  "coverUrl": "https://cdn.example.com/cover.jpg",
  "isPremium": false
}
```
coverUrl is resolved by the backend from the stored cover_path.


### 3.3 Stream Audio (HTTP Range)

```
GET /api/audios/{id}/stream
```
This endpoint supports HTTP range requests for efficient streaming and seeking.
Access control is enforced before streaming.

**Headers**

```
Range: bytes=0-1048575
```

**Response**

* `206 Partial Content`
* `Content-Type: audio/mpeg`
* Binary audio stream

Streaming details are documented in  
➡️ [`docs/streaming.md`](streaming.md)


## 4. Library API

### 4.1 Add Audio to Library

```
POST /api/library/{audioId}
```

**Response – 204 No Content**


### 4.2 Remove Audio from Library

```
DELETE /api/library/{audioId}
```

**Response – 204 No Content**


### 4.3 Get User Library

```
GET /api/library
```

**Response – 200 OK**

```json
[
  {
    "audioId": 1,
    "title": "Mindful Focus",
    "addedAt": "2025-01-10T12:00:00Z"
  }
]
```


## 5. Listening Progress API

### 5.1 Update Listening Progress

```
POST /api/progress
```

**Request**

```json
{
  "audioId": 1,
  "positionSeconds": 120
}
```

**Response – 204 No Content**


### 5.2 Get Listening Progress

```
GET /api/progress/{audioId}
```

**Response – 200 OK**

```json
{
  "audioId": 1,
  "positionSeconds": 120
}
```


## 6. Search API

### 6.1 Search Audio

```
GET /api/search?keyword=focus
```

**Response – 200 OK**

```json
[
  {
    "id": 1,
    "title": "Mindful Focus",
    "isPremium": false
  }
]
```

## 7. HTTP Status Codes

| Status | Meaning               |
| ------ | --------------------- |
| 200    | OK                    |
| 201    | Created               |
| 204    | No Content            |
| 400    | Bad Request           |
| 401    | Unauthorized          |
| 403    | Forbidden             |
| 404    | Not Found             |
| 409    | Conflict              |
| 500    | Internal Server Error |


## 8. Versioning Strategy

* URL-based versioning (`/api`)
* Breaking changes require a new version (`/api/v2`)
* Backward compatibility preserved where possible


## 9. Summary

* RESTful, JWT-secured API
* Clear separation of concerns
* HTTP Range–based audio streaming
* Designed for scalability and maintainability
* Documentation split by responsibility

This API contract is **frozen** for the current project scope.

