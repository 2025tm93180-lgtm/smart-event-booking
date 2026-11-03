# Event and inventory service

Spring Boot service for managing venues and events, creating seat inventory, and reserving, confirming, or releasing seats. It stores its data in the `event_inventory_service` MySQL database and listens on port `9091`.

## Run with Docker Compose

From the repository root:

```bash
docker compose up -d --build mysql event-service
docker compose logs -f event-service
```

The API is available at `http://localhost:9091`. In Compose, MySQL is reached at `mysql:3306`.

## Run directly

Requirements: JDK 17, Maven, and a reachable MySQL server. Start MySQL from the repository root:

```bash
docker compose up -d mysql
```

Then from `event-service/` run:

```bash
mvn spring-boot:run
```

The default database settings are in `src/main/resources/application.yaml`. Override them with Spring environment properties, for example `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `SERVER_PORT`.

## API routes

| Area | Routes |
| --- | --- |
| Events | `/api/events` (create, list, retrieve, update, delete) |
| Venues | `/api/venues` (create, list, retrieve, update) |
| Seat inventory | `/api/events/{eventId}/seats` (create and list) |
| Reservations | `/api/reservations` (reserve, retrieve, confirm, release) |
| Health | `/health` |

Request DTOs and validation rules are in the `dto/` package; controllers define the exact methods and response shapes.

## Build

```bash
mvn clean package
```

The Dockerfile builds with Maven and runs on Java 17 as a non-root user.
