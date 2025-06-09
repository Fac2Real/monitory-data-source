FROM amazoncorretto:11-alpine

VOLUME /tmp
COPY build/libs/*.jar app.jar

EXPOSE 8081 6123

ENTRYPOINT ["java", "-Xmx2G", "-jar", "/app.jar"]