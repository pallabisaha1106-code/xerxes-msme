# syntax=docker/dockerfile:1

# --- Build stage -------------------------------------------------------
# Tests are skipped here on purpose: they're unit/slice tests that don't
# need a live database (see src/test), so they're meant to run in CI
# before this image is built, not slow down every image build.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Dependencies first, in their own layer, so `docker build` only re-downloads
# them when pom.xml actually changes - not on every source edit.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package \
    && mv target/msme-backend-*.jar target/app.jar

# --- Runtime stage -------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build /app/target/app.jar app.jar
RUN chown spring:spring app.jar
USER spring

# Cloud Run sets PORT and expects the container to listen on it -
# application.yml resolves server.port from PORT (falling back to
# SERVER_PORT, then 8080) so this works unchanged locally too.
ENV PORT=8080
EXPOSE 8080

# MaxRAMPercentage instead of a fixed -Xmx: sizes the heap to whatever
# memory limit the container is actually given (e.g. a Cloud Run service
# set to 512Mi), rather than a number picked for a specific machine.
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75 -jar app.jar"]
