# Stage 1: Build the application
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime environment
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system payment && useradd --system --gid payment payment

# Copy the built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port configured in application.yaml
USER payment
EXPOSE 9090

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
