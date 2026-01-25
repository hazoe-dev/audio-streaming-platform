# 🔍 Step 7 – Search (Full‑Text Search)

## 🎯 Goal

Provide **fast, relevant search** for audios by **title & description**, using **database‑level full‑text search** instead of application‑side filtering.

This step focuses on:
- Search correctness
- Performance at scale
- Clean separation between domain logic and infrastructure


## 🧠 Design Principles

### 1️⃣ Search is *derived state*
Search data is **derived from existing fields** (`title`, `description`).

➡️ It should **not** be stored or maintained manually in application code.


### 2️⃣ Let the database do what it’s good at
PostgreSQL provides:
- `tsvector`
- `tsquery`
- GIN indexes

➡️ We leverage these instead of reinventing search logic in Java.


### 3️⃣ No triggers, no manual sync
We use a **generated column**:
- Always up‑to‑date
- No hidden logic
- No risk of data drift


## 🗄️ Database Schema

### Added column

```sql
search_vector tsvector GENERATED ALWAYS AS (
    to_tsvector(
        'english',
        coalesce(title, '') || ' ' || coalesce(description, '')
    )
) STORED
```

### Index

```sql
CREATE INDEX idx_audio_search_vector
ON audio
USING GIN (search_vector);
```

### Why GENERATED COLUMN?

| Option | Reason |
|------|-------|
| Trigger | ❌ Hidden logic, harder to reason |
| Manual UPDATE | ❌ Easy to forget, unsafe |
| Generated column | ✅ Declarative, always correct |


## 🔍 Search Query

We use **native SQL**, because JPQL does not support PostgreSQL full‑text operators (`@@`).

```sql
SELECT *
FROM audio
WHERE search_vector @@ plainto_tsquery('english', :query)
```

### Why `plainto_tsquery`?
- Safe (no syntax errors from user input)
- Automatically tokenizes words
- Good default for search boxes


## 🧩 Repository Layer

```java
@Query(
    value = """
        SELECT *
        FROM audio
        WHERE search_vector @@ plainto_tsquery('english', :query)
        """,
    nativeQuery = true
)
Page<Audio> search(String query, Pageable pageable);
```
| Roles | section |
|------     |-------|
| search_vector | Tokenized & normalized data|
| plainto_tsquery(...) | How to parse the query (AND)|
| websearch_to_tsquery(...) | How to parse the query (OR, Google-like)|


## 🌐 API Contract

### Endpoint

```
GET /api/audios/search?q={keyword}
```

### Query Parameters

| Name | Type | Description |
|----|----|------------|
| q | string | Search keyword |
| page | number | Page index |
| size | number | Page size |

### Response

Same format as `GET /api/audios`, paginated list of audio items.


## 🚫 Out of Scope (for this step)

- Ranking tuning (BM25, custom weights)
- Synonyms / multilingual search
- Fuzzy matching
- Analytics / search history

These can be added incrementally later.


## ✅ Summary

- Search is implemented at **database level**
- No triggers, no manual updates
- Clean, predictable, performant
- Easy to extend in future steps

➡️ This completes **Step 7 – Search**.

