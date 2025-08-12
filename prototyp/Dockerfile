# --- Build-Stage mit JDK 17 ---
FROM gradle:8.5-jdk17-alpine AS builder
WORKDIR /build
COPY --chown=gradle:gradle . .
RUN gradle bootJar --no-daemon

# --- Runtime-Stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--gatekeepr.rules.path=/config/rules.json"]
