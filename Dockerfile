FROM gradle:8.13-jdk17 AS builder
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN gradle build --parallel --no-daemon -x test

FROM amazoncorretto:17-alpine

VOLUME /tmp
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

EXPOSE 8081 6123

ENTRYPOINT ["java", "-Xmx2G", "-Xms1G", "-jar", "/app.jar"]