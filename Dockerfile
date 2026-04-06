# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy Maven wrapper first so dependency layer is cached separately
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Copy child module POMs (needed for reactor dependency resolution)
COPY employee-platform/pom.xml employee-platform/pom.xml
COPY employee-platform-launcher/pom.xml employee-platform-launcher/pom.xml

# Resolve all dependencies before copying source (better layer caching)
RUN ./mvnw dependency:go-offline -q

# Copy sources and build
COPY employee-platform/src employee-platform/src
COPY employee-platform-launcher/src employee-platform-launcher/src
RUN ./mvnw -q -DskipTests package

# ── Stage 2: runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /build/employee-platform-launcher/target/employee-platform-launcher-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p logs && chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
