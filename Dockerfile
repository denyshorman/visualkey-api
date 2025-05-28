FROM eclipse-temurin:24-alpine
COPY build/libs/*.jar /app/api.jar
ENTRYPOINT ["java", "-jar", "/app/api.jar"]
