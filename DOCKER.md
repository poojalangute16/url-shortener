# URL Shortener — Docker Setup

## Project Structure

```
url-shortener/
├── Dockerfile                        ← Two-stage build (JDK → JRE)
├── pom.xml
├── src/
│   ├── main/java/com/urlshortener/
│   └── test/java/com/urlshortener/
└── ...
```

---

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running
- [Docker Hub account](https://hub.docker.com/) (free) to push the image

---

## Step 1 — Build the Docker image locally

```bash
# Replace <your-dockerhub-username> with your actual Docker Hub username
docker build -t <your-dockerhub-username>/url-shortener:1.0.0 .

# Also tag as latest
docker tag <your-dockerhub-username>/url-shortener:1.0.0 \
           <your-dockerhub-username>/url-shortener:latest
```

---

## Step 2 — Run locally and verify

```bash
docker run -p 8080:8080 <your-dockerhub-username>/url-shortener:1.0.0
```

Test the endpoints:

```bash
# Shorten a URL
curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"}'

# Response:
# { "originalUrl": "...", "shortUrl": "http://localhost:8080/aB3cD4e" }

# Resolve a short code
curl http://localhost:8080/aB3cD4e

# Top 3 domains
curl http://localhost:8080/metrics/top-domains

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

## Step 3 — Push to Docker Hub

```bash
# Login to Docker Hub
docker login

# Push both tags
docker push <your-dockerhub-username>/url-shortener:1.0.0
docker push <your-dockerhub-username>/url-shortener:latest
```

Your image will be publicly available at:
```
https://hub.docker.com/r/<your-dockerhub-username>/url-shortener
```

Pull link to share in your submission:
```
docker pull <your-dockerhub-username>/url-shortener:latest
```

---

## Step 4 — Run from Docker Hub (no local build needed)

```bash
docker run -p 8080:8080 <your-dockerhub-username>/url-shortener:latest
```

---

## Optional — Pass environment variables

```bash
# Override the base URL (e.g. if running behind a reverse proxy)
docker run -p 8080:8080 \
  -e APP_BASE_URL=https://sho.rt \
  <your-dockerhub-username>/url-shortener:latest
```

---

## Dockerfile explained

| Stage | Base Image | Purpose |
|---|---|---|
| `builder` | `eclipse-temurin:17-jdk-alpine` | Compiles source, runs Maven, produces the fat JAR |
| `runtime` | `eclipse-temurin:17-jre-alpine` | Runs the JAR — no compiler/Maven, ~100MB final image |

**Security features:**
- Non-root user (`appuser`) runs the process
- JRE-only runtime — no compiler tools in production image
- `HEALTHCHECK` lets Docker/orchestrators detect unhealthy containers

**JVM flags:**
- `-XX:+UseContainerSupport` — respects Docker CPU/memory limits
- `-XX:MaxRAMPercentage=75.0` — uses up to 75% of container RAM for heap

---

## Submission checklist

- [ ] Source code link: `https://github.com/<your-username>/url-shortener`
- [ ] Docker image link: `https://hub.docker.com/r/<your-dockerhub-username>/url-shortener`
- [ ] Pull command: `docker pull <your-dockerhub-username>/url-shortener:latest`
