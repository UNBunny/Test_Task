
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY target/user-service-0.0.1-SNAPSHOT.jar user-service.jar

EXPOSE 8881

ENTRYPOINT ["java", "-jar", "/app/user-service.jar"]
