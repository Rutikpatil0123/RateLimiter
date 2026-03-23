# Stage 1: Build the application
FROM gradle:8.3-jdk21 AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

# Stage 2: Run the application
FROM eclipse-adoptium:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the gateway port (8090)
EXPOSE 8090

# Command to run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
