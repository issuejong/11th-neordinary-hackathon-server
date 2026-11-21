# ---- build ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# 1) 의존성만 먼저 받아 레이어 캐시 (build.gradle이 안 바뀌면 재사용)
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

# 2) 소스 빌드 후 Spring Boot 레이어로 분리 (의존성 / 애플리케이션)
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon \
 && java -Djarmode=tools -jar build/libs/*.jar extract --layers --launcher --destination extracted

# ---- run ----
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd -r -u 1001 spring

# 자주 안 바뀌는 레이어부터 복사 → 코드만 바뀌면 application 레이어만 새로 push
COPY --from=build /app/extracted/dependencies/ ./
COPY --from=build /app/extracted/spring-boot-loader/ ./
COPY --from=build /app/extracted/snapshot-dependencies/ ./
COPY --from=build /app/extracted/application/ ./

USER spring
ENV SPRING_PROFILES_ACTIVE=prod
# 시각(createdAt 등)을 KST로 기록 · 응답 (Cloud Run 기본은 UTC)
ENV TZ=Asia/Seoul
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=Asia/Seoul", "org.springframework.boot.loader.launch.JarLauncher"]
