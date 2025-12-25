# 1단계: 빌드 단계
FROM gradle:8.13-jdk17 AS builder

# 필요한 파일 복사
COPY --chown=gradle:gradle ./AuthServerPlatform /home/gradle/AssistAPIServer
WORKDIR /home/gradle/AssistAPIServer

# 빌드 수행
RUN gradle build -x test

# 2단계: 실행 이미지
FROM eclipse-temurin:17-jdk

# JAR 복사
COPY --from=builder /home/gradle/AssistAPIServer/build/libs/*.jar app.jar

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java -jar /app.jar"]
EXPOSE 4003
