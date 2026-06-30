# syntax=docker/dockerfile:1
# Receituário backend — Spring Boot 3 / Java 21. Multi-stage: build the jar, run on a slim JRE as a
# non-root user. Tests run in CI (Testcontainers needs a Docker daemon), NOT in the image build.
#
# The image is profile-AGNOSTIC: production behaviour is selected at run time via SPRING_PROFILES_ACTIVE=prod
# (see docker-compose.prod.yml). With no profile the app already boots the secure path (real auth + mandatory
# JWT_SECRET), backstopped by ProductionSecurityGuard.

# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
# Cache dependencies first (pom changes rarely vs src).
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests clean package

# ---- runtime stage ----
FROM eclipse-temurin:21-jre-jammy
# curl for the container HEALTHCHECK; then a non-root user.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system app && useradd --system --gid app --home /app app
WORKDIR /app
COPY --from=build /src/target/receituario-backend.jar app.jar
USER app

# Spring Boot's default port. Override with SERVER_PORT if needed.
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

# Actuator /health is exposed in the prod profile (show-details: never).
HEALTHCHECK --interval=15s --timeout=5s --start-period=70s --retries=6 \
  CMD curl -fsS http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]
