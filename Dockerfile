# ── Stage 1: build ────────────────────────────────────────────────────────────
# Use the official Maven image so we don't rely on the Maven Wrapper script,
# which has a path-extraction bug on Linux containers.
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copy POM first so dependency resolution is cached as its own layer.
# Docker only re-runs this layer when pom.xml changes, not on every source edit.
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Now copy source and build. -DskipTests because tests require a live Postgres
# database which is not available during image build.
COPY src/ src/
RUN mvn package -DskipTests -B

# ── Stage 2: runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime

# Non-root user — running as root inside a container is a security risk.
RUN groupadd --system spring && useradd --system --gid spring spring

WORKDIR /app

# Copy only the fat JAR produced by the builder stage.
COPY --from=builder /workspace/target/*.jar app.jar

# Ensure the non-root user owns the app directory.
RUN chown -R spring:spring /app
USER spring

EXPOSE 8080

# JVM flags explained:
#   -XX:MaxRAMPercentage=75.0      use up to 75% of the container memory limit for the heap
#   -Djava.security.egd=...urandom avoid blocking /dev/random on Linux; faster startup
# Spring profile is controlled by the SPRING_PROFILES_ACTIVE environment variable,
# which Spring Boot reads natively — no -D flag needed (and exec-form ENTRYPOINT
# has no shell to expand ${...} syntax anyway).
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "/app/app.jar"]
