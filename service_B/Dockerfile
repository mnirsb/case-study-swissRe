# Use an official OpenJDK runtime as the base image
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built JAR file
COPY target/*.jar app.jar

# Expose 7081 for Service B
EXPOSE 7081

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]
