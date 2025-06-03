# Stage 1: Build the application
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

# Copy project files
COPY . .

# Build the application
RUN gradle bootJar --no-daemon

# Stage 2: Create the runtime image
FROM openjdk:21-jdk-slim
WORKDIR /app

# Copy the jar from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 