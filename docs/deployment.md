# 🚀 Step 8 – Deployment

## 🎯 Goal

Deploy the **Audio Streaming Platform** at a **basic production-ready level**, suitable for demos, light traffic testing, and as a solid foundation for future scaling.

Scope includes:

- **Spring Boot** backend
- **PostgreSQL**
- **File storage** (local, S3-ready)
- **Reverse proxy – Nginx**
- A setup that is close to production **without over-engineering**

> ❗ This is **not** an enterprise-grade setup  
> ❌ No Kubernetes  
> ❌ No complex CI/CD  


## 1️⃣ Deployment Architecture 

### ✅ Phase 1 – Simple & Realistic 

```
[ Client ]
    ↓
[ Nginx ]
    ↓
[ Spring Boot API ]
    ↓
[ PostgreSQL ]
    ↓
[ File Storage (local / S3) ]
```

**Architecture mindset**:

- Nginx acts as the **single entry point**
- API focuses on business logic & streaming
- Database stores metadata only
- Audio/Cover files are kept outside the DB for easier scaling


## 2️⃣ Environment Setup

### 2.1 Environment Variables

All configuration is defined in `.env` — **nothing is hardcoded in the codebase**.

```env
# Spring profile
SPRING_PROFILES_ACTIVE=prod

# Server
SERVER_PORT=8080

# Database
DB_HOST=postgres
DB_PORT=5432
DB_NAME=audiostreaming
DB_USER=audio_user
DB_PASSWORD=secret

# JPA
JPA_SHOW_SQL=false

# JWT
JWT_SECRET=MSOt3eJZ+Zq/U+dzQ0j9lT3A5Vdo41uhLKYVMRAVdRw=
JWT_ACCESS_EXP=900
JWT_REFRESH_EXP=2592000
JWT_ISSUER=audiostreaming

# App
APP_COVER_BASE_URL=http://localhost
APP_AUDIO_BASE_URL=/storage
```

👉 Principles:
- The application **does not know** where it is running
- Switching environments = changing `.env` only


## 3️⃣ Dockerization

### 3.1 Dockerfile (Spring Boot – Multi-stage build)

```dockerfile
# Build stage
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew dependencies

COPY src src
RUN ./gradlew build -x test

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","app.jar"]
```

**Why multi-stage?**

- Smaller final image
- No Gradle or source code in runtime image
- Faster startup, production-friendly


### 3.2 docker-compose.yml

```yaml
version: "3.9"

services:
  nginx:
    image: nginx:latest
    ports:
      - "80:80"
    volumes:
      - ./storage:/storage
      - ./nginx.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - api

  postgres:
    image: postgres:18
    environment:
      POSTGRES_DB: audiostreaming
      POSTGRES_USER: audio_user
      POSTGRES_PASSWORD: secret
    volumes:
      - pgdata:/var/lib/postgresql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U audio_user"]
      interval: 5s
      timeout: 5s
      retries: 5

  api:
    build: .
    env_file:
      - .env
    environment:
      SPRING_PROFILES_ACTIVE: prod
    depends_on:
      postgres:
        condition: service_healthy
    expose:
      - "8080"
    volumes:
      - ./storage:/storage

volumes:
  pgdata:
```

**Key points**:

- `api` is **not exposed to the host** → only Nginx is public
- `depends_on + healthcheck` ensures DB readiness before API startup
- Storage is shared between API and Nginx

---

## 4️⃣ Nginx (Reverse Proxy)

### 4.1 Why Nginx?

- TLS termination (HTTPS-ready)
- Serve static files (cover images)
- Correctly forward **HTTP Range Requests** for audio streaming
- More production-like than exposing Spring Boot directly

### 4.2 Nginx Config (Critical for streaming)

```nginx
server {
    listen 80;

    location / {
        proxy_pass http://api:8080;
        proxy_set_header Host $host;
        proxy_set_header Range $http_range;
        proxy_set_header If-Range $http_if_range;
    }

    location /cover/ {
        alias /storage/cover/;
        autoindex off;
    }
}
```

⚠️ **Forwarding Range headers is mandatory**

- Missing this → audio seeking/streaming will break
- Correctly implemented since Step 4 👍

## 5️⃣ Storage Strategy

### 5.1 Folder Structure

```
/storage/
├── audio/
│   └── 2026/01/mindful_focus.mp3
└── cover/
    └── 2026/01/mindful_focus.jpg
```

### 5.2 Database stores paths only

```text
audio_path = audio/2026/01/mindful_focus.mp3
cover_path = cover/2026/01/mindful_focus.jpg
```

**Benefits**:

- Lightweight database
- Faster queries
- Easy storage migration
- No tight coupling to local disk

## 🔮 Future – Phase 2 (Optional)

| Layer   | Current | Upgrade | Code change |
|--------|---------|---------|-------------|
| Storage | Local FS | S3 | ❌ |
| CDN     | — | CloudFront | ❌ |
| DB      | Postgres | Postgres | ❌ |

👉 Designed from day one so **storage can be replaced without touching business logic**


## 6️⃣ Database Initialization Strategy (Spring Boot 4)

This project uses **Flyway as the single source of truth for database schema and data initialization**.
JPA/Hibernate is **not** responsible for creating or modifying database structures in production.

### Responsibilities Overview

| Component           | Responsibility                                                        |
| ------------------- | --------------------------------------------------------------------- |
| **Flyway**          | Create tables, sequences, indexes, constraints, and seed initial data |
| **JPA / Hibernate** | Object–relational mapping and schema validation only                  |
| **data.sql**        | Disabled in production                                                |

---

### Migration Flow on Application Startup

The application follows the default Spring Boot 4 startup order:

```
Flyway migrations
→ Hibernate schema validation (JPA)
→ Application startup
```

This guarantees that:

* All required database objects exist before Hibernate initializes
* Schema drift is detected early via validation
* No runtime schema changes occur in production


### Flyway Migration Structure

All migrations are located under:

```
src/main/resources/db/migration
```

Example:

```
db/migration/
├── V1__create_tables.sql        # Tables, sequences, constraints, indexes
├── V3__seed_initial_data.sql   # Initial users / reference data
├── V7__add_full_text_search.sql
```

**Key rules:**

* Every structural change must be introduced via a new Flyway version
* Seed data is handled by Flyway, not `data.sql`
* No manual SQL execution on production databases


### JPA Configuration (Production)

In production, Hibernate is restricted to validation only:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate   # or none
```

This ensures:

* Hibernate does not create, drop, or alter schema
* The application fails fast if the database schema is incompatible


### SQL Initialization Policy

```yaml
spring:
  sql:
    init:
      mode: never
```

* `data.sql` is disabled in production
* All data initialization is versioned and controlled by Flyway
* Prevents accidental data duplication or destructive re-runs

### Rationale

This strategy provides:

* **Deterministic deployments** across environments
* **Versioned, auditable schema evolution**
* **Safe production startups** with no implicit schema mutation
* Clear separation between **schema management** and **ORM mapping**


## What NOT to Do 

❌ Kubernetes  
❌ Elasticsearch  
❌ Kafka  
❌ CI/CD pipelines  
❌ Microservices  

## 7️⃣ Production Checklist

### ✅ Security

* JWT secret via env
* No dev endpoints
* CORS configured

### ✅ Performance

* HTTP Range streaming
* GIN index for search
* Pagination everywhere

### ✅ Observability (basic)

* Access logs
* Error logs
* Health endpoint

```
GET /actuator/health
```

```bash 
docker logs -f audiostreaming-nginx-1
```

Access log output
```shell
172.20.0.1 - - [31/Jan/2026:08:43:42 +0000] "GET /actuator/health HTTP/1.1" 200 60 "-" "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36" "-"
```
## ✅ Summary

- Realistic setup for small-scale production
- Simple but **architecturally correct**
- Ready to evolve when needed

🎧 *A streaming platform doesn’t need to be complex — it just needs to be built right.*

