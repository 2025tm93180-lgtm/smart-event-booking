FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system event && useradd --system --gid event event
COPY --from=build /app/target/event-inventory-service-*.jar app.jar
USER event
EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app.jar"]
