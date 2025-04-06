
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY target/company-service-0.0.1-SNAPSHOT.jar company-service.jar

EXPOSE 8882

ENTRYPOINT ["java", "-jar", "/app/company-service.jar"]
