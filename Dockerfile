# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradle gradle
COPY gradlew gradlew
COPY build.gradle build.gradle
COPY src src

RUN sed -i 's/\r$//' ./gradlew \
    && chmod +x ./gradlew \
    && ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:25-jdk
WORKDIR /app

RUN useradd --system --create-home --home-dir /app realworld
COPY --from=build /workspace/build/libs/*.jar /app/realworld.jar
RUN chown realworld:realworld /app/realworld.jar

USER realworld
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/realworld.jar"]
