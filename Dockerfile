# ── Build stage ──────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Cache dependencies separately from source
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn package -DskipTests -B -q

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

COPY --from=build /app/target/document-management-*.jar app.jar

# JVM flags tuned for memory-constrained containers.
# -XX:+UseContainerSupport  → respect cgroup memory limits
# -XX:MaxRAMPercentage=75.0 → use 75 % of container RAM as heap ceiling
# Large uploads never exhaust the heap because multipart files are spooled to
# disk (file-size-threshold=0) and then streamed to MinIO; heap stays flat.
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]

EXPOSE 8080
