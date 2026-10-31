FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system ticket && useradd --system --gid ticket ticket
COPY --from=build /app/target/ticket-notification-service-1.0.0.jar app.jar
USER ticket
EXPOSE 8084
ENTRYPOINT ["java","-jar","app.jar"]
