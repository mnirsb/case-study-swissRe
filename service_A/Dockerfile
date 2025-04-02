# Use an official OpenJDK runtime as the base image
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built JAR file 
COPY target/*.jar app.jar

# Expose 7080 for Service A
EXPOSE 7080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]
