FROM openjdk:21-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the fat JAR file into the container
COPY build/libs/*.jar app.jar

# Expose the port the application runs on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
