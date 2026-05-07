# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

# Copy the Maven wrapper and POM first so dependency resolution is cached as its
# own layer. Docker only re-runs this layer when pom.xml or the wrapper changes,
# not on every source edit.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Now copy source and build. -DskipTests because tests require a live Postgres
# database which is not available during image build.
COPY src/ src/
RUN ./mvnw package -DskipTests -B

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
#   -XX:MaxRAMPercentage=75.0          use up to 75% of the container memory limit for the heap
#   -Djava.security.egd=...urandom     avoid blocking /dev/random on Linux; faster startup
#   -Dspring.profiles.active=...       default to prod profile; override via SPRING_PROFILES_ACTIVE env var
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}", \
  "-jar", "/app/app.jar"]
