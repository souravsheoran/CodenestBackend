# Use the official OpenJDK image
FROM openjdk:17-jdk-slim AS base

# Set the working directory in the container
WORKDIR /app

# Copy the Maven wrapper, project files, and .mvn folder to the container
COPY .mvn/ .mvn
COPY mvnw .
COPY pom.xml .

# Run Maven to download dependencies (without the source code, which will be copied later)
RUN ./mvnw dependency:go-offline

# Copy the source code
COPY src ./src

# Build the project
RUN ./mvnw clean package

# Set environment variables for database connection
ENV DB_HOST=jdbc:mysql://junction.proxy.rlwy.net
ENV DB_PORT=55278
ENV DB_NAME=railway
ENV DB_USER=root
ENV DB_PASSWORD=oTtyyPCEfrBgVCYomkyemxWsciMUCWuP

# Expose port 8080 for the Spring Boot application
EXPOSE 8080

# Run the Spring Boot app
CMD ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]
