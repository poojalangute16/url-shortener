# ─────────────────────────────────────────────────────────────────────────────
# Stage 1: Build
# Uses the full JDK image to compile and package the application with Maven.
# The Maven wrapper (mvnw) is used so no Maven installation is required.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first — Docker caches this layer separately
# so dependencies are only re-downloaded when pom.xml changes.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Pre-download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code and build the fat JAR (skip tests — run them separately in CI)
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2: Runtime
# Uses a slim JRE-only image — no compiler, no Maven, minimal attack surface.
# Final image is ~100MB instead of ~350MB with the full JDK.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Create a non-root user for security best practice
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy only the built JAR from the builder stage
COPY --from=builder /app/target/url-shortener-1.0.0.jar app.jar

# Set ownership to non-root user
RUN chown appuser:appgroup app.jar

USER appuser

# Expose the application port
EXPOSE 8080

# Health check — Docker marks the container healthy once Spring Boot is ready
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# JVM tuning for containers:
#   -XX:+UseContainerSupport     — respects Docker memory/CPU limits
#   -XX:MaxRAMPercentage=75.0    — uses up to 75% of container memory for heap
#   -Djava.security.egd=...      — faster startup (avoids blocking /dev/random)
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
