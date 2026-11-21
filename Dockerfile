# ---- build ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# ---- run ----
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd -r -u 1001 spring
COPY --from=build /app/build/libs/*.jar app.jar
USER spring
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
