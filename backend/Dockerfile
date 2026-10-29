# syntax=docker/dockerfile:1

# ---------- Stage 1: build the jar ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy only the POM first so the dependency layer is cached until pom.xml changes
COPY pom.xml .
COPY config config
RUN mvn -B -q dependency:go-offline

COPY src src
# Tests + lint run in CI; the image build only needs the jar
RUN mvn -B -q -DskipTests -Dcheckstyle.skip=true -Djacoco.skip=true package

# ---------- Stage 2: tiny runtime image ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl is used by the docker-compose healthcheck
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app app

COPY --from=build /build/target/*.jar app.jar
USER app

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fs http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
