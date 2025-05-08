FROM gradle:8.13-jdk17 AS builder
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN gradle build --no-daemon -x test

FROM amazoncorretto:17-alpine

VOLUME /tmp
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

COPY src/main/resources/application.properties /app/config/application.properties
COPY resources/cert /app/config/cert

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.profiles.active=dev"]