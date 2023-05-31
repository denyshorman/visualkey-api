FROM openjdk:21-slim
COPY build/libs/*.jar /app/api.jar
COPY src/main/resources/application.conf /app/application.conf
CMD ["java", "-jar", "/app/api.jar", "-config=/app/application.conf"]
