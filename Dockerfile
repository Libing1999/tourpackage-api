# syntax=docker/dockerfile:1

# ---- Build ----
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline

COPY src ./src
RUN ./mvnw -B clean package -DskipTests

# ---- Runtime ----
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

# The upload directory has to exist in the image, owned by the runtime user.
# Docker creates a missing volume mount point as root, which would leave the
# non-root process unable to write a single upload; when the directory is
# already present, a new named volume inherits its ownership instead.
RUN mkdir -p /app/uploads && chown -R spring:spring /app

USER spring

COPY --from=builder --chown=spring:spring /workspace/target/tourpackage-api.jar app.jar

EXPOSE 8080

# Reports to the orchestrator whether this instance can serve traffic. The
# readiness group includes the database, so a container that started but cannot
# reach Postgres is correctly reported as not ready rather than as healthy.
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/api/actuator/health/readiness | grep -q '"status":"UP"' || exit 1

# MaxRAMPercentage rather than a fixed -Xmx: the JVM would otherwise size its
# heap from the host's memory, not the container limit, and get OOM-killed by
# the runtime on a box far larger than its cgroup allows.
# ExitOnOutOfMemoryError makes an exhausted heap a restart rather than a process
# that stays up serving errors.
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "/app/app.jar"]
