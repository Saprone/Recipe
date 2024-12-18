# Use a base image to build the application
FROM openjdk:21-jdk-slim AS builder

# Set the working directory in the container
WORKDIR /app

# Copy the Maven build files
COPY pom.xml ./

# Copy the source code
COPY src ./src

# Install Maven
RUN apt-get update && apt-get install -y maven

# Build the application
RUN mvn clean package -DskipTests

# Use a new image to run the application
FROM openjdk:21-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the built JAR file from the builder stage
COPY --from=builder /app/target/*.jar recipe.jar

# Expose the application port
EXPOSE 8084

# Run the application
ENTRYPOINT ["java", "-jar", "recipe.jar"]