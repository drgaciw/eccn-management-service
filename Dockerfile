# Stage 1: build
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# Copy the Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Copy the source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests

# Stage 2: runtime
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Create a non-root user
RUN groupadd -r app && useradd -r -g app app

# Copy the built artifact from the build stage
COPY --from=build /app/target/eccn-management-service-0.0.1-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8080

# Run as a non-root user
USER app

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]