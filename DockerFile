# Stage 1: Build the application using Maven
FROM maven:3.9.9-amazoncorretto-23 AS build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM amazoncorretto:23-alpine-jdk
# Copy the jar file from the build stage
COPY --from=build /target/*.jar app.jar

# Expose the port Spring Boot runs on
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]