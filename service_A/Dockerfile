# Use an official OpenJDK runtime as the base image
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built JAR file (adjust the JAR name as per your build output)
COPY target/*.jar app.jar

# Expose 7080 for Service A
EXPOSE 7080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]