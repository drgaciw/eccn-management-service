# Use official OpenJDK 21 image
FROM eclipse-temurin:21-jdk-jammy

# Set working directory
WORKDIR /app

# Copy the Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Copy the source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "target/eccn-management-service-0.0.1-SNAPSHOT.jar"]