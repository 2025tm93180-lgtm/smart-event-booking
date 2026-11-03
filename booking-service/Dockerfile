FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system booking && useradd --system --gid booking booking
COPY --from=build /app/target/booking-service-*.jar app.jar
USER booking
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
