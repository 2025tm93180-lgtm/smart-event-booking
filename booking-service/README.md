# Booking service

Spring Boot service that accepts booking requests, coordinates seat reservations with the Event service, tracks the booking Saga, and consumes payment events from Kafka. It persists booking, seat, idempotency, Saga, and outbox data in the `booking_db` MySQL database.

## Run with Docker Compose

From the repository root:

```bash
docker compose up -d --build mysql kafka booking-service
docker compose logs -f booking-service
```

The service is available at `http://localhost:8081`. In Compose, its database and Kafka endpoints are `mysql:3306` and `kafka:19092`.

## Run directly

Requirements: JDK 21, Maven, and reachable MySQL and Kafka instances. Start the shared dependencies from the repository root:

```bash
docker compose up -d mysql kafka
```

Then from `booking-service/`, configure the connection values for your local MySQL/Kafka if they differ from the defaults and run:

```bash
mvn spring-boot:run
```

The default local settings are in `src/main/resources/application.properties`. The service uses port `8081`, database `booking_db`, Kafka bootstrap server `localhost:9092`, and the `payment-events` topic. Use environment variables such as `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `SPRING_KAFKA_BOOTSTRAP_SERVERS` to override Spring settings.

## Main API

The booking controller is rooted at `/api/bookings`:

- `POST /api/bookings` creates a booking.
- `GET /api/bookings/{bookingId}` retrieves a booking.
- `GET /api/bookings` lists bookings.

See `controller/BookingController.java` and its request/response DTOs for the current payloads. The service also publishes booking events from its outbox and consumes payment events from Kafka.

## Build

```bash
mvn clean package
```

The Dockerfile builds the application with Maven and runs the packaged JAR on Java 21.
