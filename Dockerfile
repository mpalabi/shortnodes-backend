FROM eclipse-temurin:21-jdk-alpine as build
WORKDIR /workspace
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src
# Use BuildKit cache to avoid re-downloading Gradle every build
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew --build-cache bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

