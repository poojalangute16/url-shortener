# URL Shortener Service

A REST API built in **Java 17 + Spring Boot 3.2** that shortens URLs, redirects short links to their original destinations, and exposes domain-level usage metrics.

Interactive API documentation is available via **Swagger UI** at `http://localhost:8080/swagger-ui.html` once the server is running.

---

## Table of Contents

1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Prerequisites](#prerequisites)
4. [Getting the Code](#getting-the-code)
5. [Running Locally](#running-locally)
6. [Running Tests](#running-tests)
7. [API Reference](#api-reference)
8. [OpenAPI / Swagger UI](#openapi--swagger-ui)
9. [Docker](#docker)
10. [Project Structure](#project-structure)
11. [Design Decisions & Assumptions](#design-decisions--assumptions)
12. [Known Limitations](#known-limitations)

---

## Features

- **Shorten a URL** — submit any valid URL and receive a 7-character short code
- **Idempotent shortening** — submitting the same URL twice returns the same short URL, not a new one
- **Redirect** — visiting the short URL redirects the browser/client to the original URL via HTTP 302
- **Metrics** — query the top 3 most-shortened domains at any time
- **In-memory storage** — no database setup required; everything lives in the running process
- **OpenAPI 3.0 docs** — full Swagger UI with try-it-out support and raw JSON spec
- **Dockerized** — includes a Dockerfile for containerized deployment

---

## Tech Stack

| Layer      | Technology                         |
| ---------- | ---------------------------------- |
| Language   | Java 17                            |
| Framework  | Spring Boot 3.2                    |
| Build tool | Maven 3.8+                         |
| Storage    | In-memory (`ConcurrentHashMap`)    |
| API Docs   | SpringDoc OpenAPI 2.3 (Swagger UI) |
| Testing    | JUnit 5, AssertJ, Spring MockMvc   |
| Container  | Docker (optional)                  |

---

## Prerequisites

Make sure the following are installed before running the project:

| Tool       | Minimum version | Check              |
| ---------- | --------------- | ------------------ |
| Java (JDK) | 17              | `java -version`    |
| Maven      | 3.8             | `mvn -version`     |
| Docker     | 20.x (optional) | `docker --version` |

> **Important:** The project uses Java 17 language features. Running on Java 11 or below will fail at compile time. Always confirm with `java -version` before proceeding.

---

## Getting the Code

```bash
git clone https://github.com/<your-username>/url-shortener.git
cd url-shortener
```

---

## Running Locally

### Option A — Maven wrapper (recommended)

The Maven wrapper (`mvnw`) is bundled in the repo so you do **not** need Maven installed globally.

```bash
# macOS / Linux
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

### Option B — System Maven

```bash
mvn spring-boot:run
```

The server starts on **`http://localhost:8080`**. You will see this line in the logs when it is ready:

```
Started UrlShortenerApplication in X.XXX seconds
```

### Changing the port

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
```

### Changing the base URL for generated short links

By default short URLs look like `http://localhost:8080/aB3cD4e`. If you deploy behind a custom domain:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--app.base-url=https://sho.rt
```

---

## Running Tests

```bash
./mvnw test
```

This runs both test suites:

| Test class                   | Type                  | What it covers                                                          |
| ---------------------------- | --------------------- | ----------------------------------------------------------------------- |
| `UrlShortenerServiceTest`    | Unit                  | Shortening logic, idempotency, resolution, validation, metrics ordering |
| `UrlShortenerControllerTest` | Integration (MockMvc) | All REST endpoints, HTTP status codes, headers, error responses         |

Run a single test class:

```bash
./mvnw test -Dtest=UrlShortenerServiceTest
./mvnw test -Dtest=UrlShortenerControllerTest
```

---

## API Reference

All request/response bodies use `Content-Type: application/json`.

---

### 1. Shorten a URL

**`POST /shorten`**

Accepts a full URL and returns a shortened version. Calling this endpoint multiple times with the same URL always returns the **same** short URL (idempotent).

**Request body:**

```json
{
  "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
}
```

**Success — `201 Created`:**

```json
{
  "originalUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  "shortUrl": "http://localhost:8080/aB3cD4e"
}
```

**Error — `400 Bad Request`** (invalid or malformed URL):

```
URL must include a scheme and host: youtube-dot-com
```

**curl:**

```bash
curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"}'
```

---

### 2. Redirect to Original URL

**`GET /{shortCode}`**

Redirects the caller to the original URL. Browsers follow the redirect automatically.

**Success — `302 Found`:**

```
Location: https://www.youtube.com/watch?v=dQw4w9WgXcQ
```

**Error — `404 Not Found`** (unknown short code):

```
Short code not found: unknown1
```

**curl:**

```bash
# Follow the redirect
curl -L http://localhost:8080/aB3cD4e

# Inspect the redirect headers without following
curl -v http://localhost:8080/aB3cD4e
```

---

### 3. Top Domains Metrics

**`GET /metrics/top-domains`**

Returns the top 3 domains by total number of URLs shortened, ordered highest first.

> `www.` is stripped when grouping — `www.youtube.com` and `youtube.com` count as the same domain.

**Success — `200 OK`:**

```json
{
  "udemy.com": 6,
  "youtube.com": 4,
  "wikipedia.org": 2
}
```

**curl:**

```bash
curl http://localhost:8080/metrics/top-domains
```

---

### End-to-end curl walkthrough

```bash
# Shorten several URLs
curl -s -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://udemy.com/course/java-masterclass"}'

curl -s -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://udemy.com/course/spring-boot"}'

curl -s -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://youtube.com/watch?v=abc"}'

# Same URL again — returns the identical short URL
curl -s -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://udemy.com/course/java-masterclass"}'

# Follow a redirect (replace aB3cD4e with your actual short code)
curl -L http://localhost:8080/aB3cD4e

# Check metrics
curl http://localhost:8080/metrics/top-domains
```

---

## OpenAPI / Swagger UI

SpringDoc OpenAPI is included out of the box. Once the server is running:

| URL                                      | Description                                                |
| ---------------------------------------- | ---------------------------------------------------------- |
| `http://localhost:8080/swagger-ui.html`  | Interactive Swagger UI — try every endpoint in the browser |
| `http://localhost:8080/v3/api-docs`      | Raw OpenAPI 3.0 JSON spec                                  |
| `http://localhost:8080/v3/api-docs.yaml` | Raw OpenAPI 3.0 YAML spec                                  |

### What the Swagger UI gives you

- **Try it out** — execute any endpoint directly from the browser with no extra tooling
- **Request/response schemas** — field-level descriptions and examples for every DTO
- **Response codes** — all possible HTTP status codes documented per endpoint
- **Example values** — pre-filled request bodies so you can test with one click

### Importing into Postman or Insomnia

1. Open Postman → Import → Link
2. Paste `http://localhost:8080/v3/api-docs`
3. Postman generates a full collection with all three endpoints ready to use

## Docker

### Build the image

```bash
docker build -t url-shortener:latest .
```

### Run the container

```bash
docker run -p 8080:8080 url-shortener:latest
```

Swagger UI will be available at `http://localhost:8080/swagger-ui.html`.

### Run with a custom base URL

```bash
docker run -p 8080:8080 \
  -e APP_BASE_URL=https://sho.rt \
  url-shortener:latest
```

### Run on a custom port

```bash
docker run -p 9090:9090 \
  -e SERVER_PORT=9090 \
  url-shortener:latest
```

### How the Dockerfile works

The Dockerfile uses a **two-stage build**:

1. **Stage 1 (`build`)** — A full JDK Alpine image compiles the source and packages the JAR with Maven
2. **Stage 2 (runtime)** — Only the compiled JAR is copied into a slim JRE Alpine image

This keeps the final image small by excluding the JDK, Maven, and source code from the deployed artifact.

---

## Design Decisions & Assumptions

**Short code generation** — A random 7-character code is picked from a 62-character alphabet (a–z, A–Z, 0–9), giving 62⁷ ≈ 3.5 trillion possible codes. Each code is checked for collisions before being saved; regeneration is triggered if one is found (extremely unlikely in practice).

**Idempotency** — The repository maintains a reverse index (`originalUrl → ShortenedUrl`) so the lookup before creating a new entry is O(1), not a full scan.

**`www.` stripping** — Happens at shorten-time so the domain stored in the model is always the canonical form. This ensures `www.youtube.com` and `youtube.com` are grouped correctly without any runtime transformation in the metrics query.

**Thread safety** — `ConcurrentHashMap` is used instead of `HashMap` to safely handle concurrent HTTP requests without explicit locking.

**OpenAPI-first documentation** — All annotations live on the controller and DTOs rather than in a separate YAML file, keeping the documentation co-located with the code it describes and ensuring they stay in sync.

---

## Known Limitations

- **Data is not persisted.** Restarting the application clears all shortened URLs. A production version would use a database (e.g., PostgreSQL or Redis).
