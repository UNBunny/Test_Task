
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY target/config-server-0.0.1-SNAPSHOT.jar config-server.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "/app/config-server.jar"]
