FROM eclipse-temurin:25-jdk-jammy AS builder

WORKDIR /workspace
COPY AuthServerPlatform/ ./
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:25-jre-jammy

RUN groupadd --system app && useradd --system --gid app --home-dir /app app
WORKDIR /app
COPY --from=builder --chown=app:app /workspace/build/libs/*.jar app.jar

USER app
EXPOSE 4003
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
