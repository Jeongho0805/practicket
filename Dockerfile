FROM openjdk:17-jdk-slim

WORKDIR /app

COPY build/libs/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]