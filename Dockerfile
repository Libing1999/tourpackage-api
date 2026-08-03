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

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
