# ✅ Spring Boot 4 MVC Test — Practical Summary

## 1️⃣ What `@WebMvcTest` does

```java
@WebMvcTest(AuthController.class)
```

* Loads **only MVC-related beans**

    * Controllers
    * Jackson (`ObjectMapper`)
    * Validation
    * `HttpMessageConverter`
* ❌ Does **not** load:

    * Services
    * Repositories
    * Database
    * Embedded server

➡️ Runs in a **mock servlet environment** (no real HTTP server).

---

## 2️⃣ Mocking dependencies (Boot 4 way)

```java
@MockitoBean
private AuthService authService;
```

* Replaces a Spring bean with a Mockito mock
* Official replacement for `@MockBean` in Boot 4
* Used to isolate controller logic

---

## 3️⃣ Performing requests

```java
@Autowired
MockMvcTester mvc;
```

* Boot 4’s fluent MVC test client
* No `andExpect(...)`
* Uses AssertJ

Example:

```java
mvc.post()
   .uri("/api/auth/register")
   .contentType(MediaType.APPLICATION_JSON)
   .content(jsonBody);
```

⚠️ `Content-Type` is **mandatory** for `@RequestBody`
→ missing it results in **415 Unsupported Media Type**

---

## 4️⃣ What you can assert (IMPORTANT)

The returned assertion type is:

```
MvcTestResultAssert
```

### ✅ Supported assertions

* `hasStatus(HttpStatus)`
* `hasStatusOk()`, `hasStatus4xxClientError()`, etc.
* `hasHeader(String, String)`
* `hasBodyTextEqualTo(String)`

### ❌ Not supported

* JSONPath assertions
* `hasBodyJson(...)`
* `hasJsonPath(...)`
* Predicate-based body assertions

This is **intentional design** in Boot 4.

---

## 5️⃣ How to assert JSON responses correctly

### ✅ Option A — Strict body check (simple, but fragile)

```java
assertThat(result)
    .hasStatus(HttpStatus.CREATED)
    .hasBodyTextEqualTo("""
        {"username":"username123","message":"User registered successfully"}
    """);
```

* Field order matters
* Whitespace matters
* Best for **contract tests**

---

### ✅ Option B — Recommended (extract and assert)

```java
var result = mvc.post()
        .uri("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .exchange();

assertThat(result.getResponse().getStatus())
        .isEqualTo(HttpStatus.CREATED.value());

RegisterResponse response =
        objectMapper.readValue(
            result.getResponse().getContentAsString(),
            RegisterResponse.class);

assertThat(response.username()).isEqualTo("username123");
```

✔ Type-safe
✔ Formatting-independent
✔ Most robust approach

---

## 6️⃣ Spring Security & 403 errors

If Spring Security is present:

```java
@AutoConfigureMockMvc(addFilters = false)
```

* Disables security filters
* Prevents 403 in controller tests

---

## 7️⃣ When NOT to use `MockMvcTester`

If you need:

* JSONPath
* Flexible JSON assertions
* Matcher-style expectations

➡️ Use **classic `MockMvc`** (still fully supported in Boot 4).

---

## 8️⃣ Official references (trusted)

* Spring Boot testing:
  - https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html
  
* Spring MVC testing:
  - https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.spring-mvc-tests

---

# ✅ Final Takeaway

> **Spring Boot 4 `MockMvcTester` is intentionally minimal.**
> Assert status + headers directly, and **deserialize response bodies manually** for structured checks.

This keeps tests:

* Explicit
* Type-safe
* Future-proof

