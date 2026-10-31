# Ticket & Notification Service — Member 4

Java 21 / Spring Boot 3.5.7 / MySQL 8.4 / Apache Kafka. Port **8084**. Implements Section 8 of the supplied Smart Event Ticket Booking System Blueprint. This project follows the PDF's **Kafka** decision.

## macOS: Java 21 and Maven inside this folder

From the extracted project folder, run:

```bash
python3 scripts/install_tools.py
bash mvn-local -version
bash mvn-local test
```

The installer detects Intel or Apple Silicon and downloads Eclipse Temurin Java 21 and Apache Maven 3.9.11 into `.tools/`. Downloads are checksum-verified. Python 3, curl, tar, and internet access are required; curl and tar are built into macOS. No Homebrew, sudo, global Java/Maven installation, or shell-profile changes are needed. An existing local tool installation is reused.

`mvn-local` sets JAVA_HOME for that command and keeps Maven's dependency cache inside `.tools/m2/repository`. Use it in place of `mvn`:

```bash
bash mvn-local clean package
bash mvn-local spring-boot:run
```

Running the app still requires MySQL, Kafka, and the environment variables described below. Docker is required for the MySQL Testcontainers test; otherwise that test is skipped. The installer contains no preinstalled macOS binaries: the download and installation take place on your Mac when you run it. Keep `.tools/` out of Git. The installer also supports Linux; Windows is not supported by this script.

## Real email delivery

See **[docs/EMAIL_SETUP.md](docs/EMAIL_SETUP.md)** for SMTP settings, a fresh booking-email demo, local inbox capture, and safe manual retries. Default mode remains `mock`. In `smtp` mode, each ticket is emailed to `payload.customerEmail` from the trusted Booking event. SMTP acceptance is tracked separately from uncertain delivery outcomes. Existing data upgrades through additive Flyway migration V2.

## Quick start: Docker Desktop

Requirements: Docker Desktop with Compose v2, Python 3.10+ (helper scripts only). No host Java or Maven needed for this route.

From the project directory:

```bash
python3 scripts/setup_env.py
docker compose up -d --build
docker compose logs -f app
```

Wait for `Started TicketNotificationApplication`, then use another terminal:

```bash
curl http://localhost:8084/health
python3 scripts/smoke_test.py
```

The smoke script publishes the same two-seat booking repeatedly, verifies two stable ticket IDs, checks authentication/ownership/pagination, and waits for two simulated notifications. It can be rerun. Use the normal `MOCK_NOTIFICATION_FAIL=false` configuration for it.

Stop without deleting your database:

```bash
docker compose down
```

Port conflicts: if your team already runs MySQL/Kafka on 3306/9092, use those dependencies and the host run instructions below, or change the Compose host port mappings and advertised EXTERNAL Kafka listener together. Do not run two brokers on the same host port.

## Explore with Postman

Import `examples/postman_collection.json`. Generate a local token:

```bash
python3 scripts/dev_token.py
```

Paste it into the collection's `customerToken` variable. For retries, generate an admin token with `python3 scripts/dev_token.py --role ADMIN` and set `adminToken`. Publish the sample:

```bash
python3 scripts/publish_event.py
```

Use these endpoints:

| Method | Path | Access / behavior |
| --- | --- | --- |
| GET | `/health` | Public health, 200 or 503 |
| GET | `/api/v1/tickets/{ticketId}` | Owner or ADMIN |
| GET | `/api/v1/tickets/by-booking/{bookingId}?page=0&size=20` | Owner or ADMIN; stable seat sort |
| GET | `/api/v1/notifications/by-booking/{bookingId}?page=0&size=20` | Owner or ADMIN |
| POST | `/api/v1/notifications/retry/{attemptId}` | ADMIN; FAILED → PENDING; returns 202 |

Lists return `{items,page,size,total}`. Page starts at 0; size is 1–100. Missing bookings return 404, including before their confirmation has been consumed. Wrong owner returns 403. A pending/sent notification cannot be retried (409). Ticket creation is event-driven. Real SMTP email is available using `NOTIFICATION_MODE=smtp`; see `docs/EMAIL_SETUP.md`. Ticket cancellation, manual issuance, and real SMS are not implemented.

The OpenAPI 3 specification is `docs/openapi.yaml`, also served at authenticated `GET /openapi.yaml`. Import it into Postman or Swagger Editor. There is no bundled Swagger UI.

## Run with Java / IDE instead

Requirements: JDK 21, Maven 3.9+, Docker (or your team's existing MySQL 8/Kafka).

```bash
python3 scripts/setup_env.py
docker compose up -d mysql kafka
```

Set these environment variables in your IDE's run configuration using values from `.env`:

- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_ISSUER=smart-event-booking`
- `JWT_AUDIENCE=ticket-api`

Defaults connect to `localhost:3306/ticket_db` as `ticket_user` and `localhost:9092`. For custom hosts set `DB_URL`, `DB_USERNAME`, `KAFKA_BOOTSTRAP_SERVERS`. Spring Boot does not automatically load `.env`; Compose does.

Then run `TicketNotificationApplication`, or:

```bash
mvn spring-boot:run
```

On macOS/Linux only, you can export the generated `.env` first:

```bash
set -a
. ./.env
set +a
mvn spring-boot:run
```

Build a runnable JAR with `mvn clean package`. The Docker image builds with Java 21 and runs as a non-root user.

## Test

```bash
mvn test
```

- `ServiceIntegrationTest`: fast H2-in-MySQL-mode tests of real Spring services, migrations, transactions, HTTP authorization, real signed JWT validation, and 20 concurrent logical duplicates.
- `SmtpDeliveryTest`: actual local SMTP submission/content, duplicate protection, missing email, ambiguous acknowledgment, expired claims, concurrency, and header validation. Sends no email externally.
- `KafkaFlowTest`: embedded Kafka exercises the real listener, duplicates, malformed-event DLT handling, and offset recovery using H2.
- `MySqlConcurrencyTest`: real MySQL 8.4 via Testcontainers verifies migration syntax, 20 concurrent confirmations, and notification row locking. Requires Docker; automatically **skips** when Docker is unavailable. A skipped test is not MySQL validation.
- `scripts/smoke_test.py`: real Compose application, Kafka, and MySQL end-to-end check; run separately after startup.

See `docs/VALIDATION.md` for what was actually run when this project was generated.

## Demonstrate a notification failure and retry

1. Set `MOCK_NOTIFICATION_FAIL=true` in `.env`; recreate the app with `docker compose up -d app`.
2. Copy `examples/booking-confirmed.json` to a new local JSON file; set **both** a new `eventId` and a new `bookingId`, then publish that file with `python3 scripts/publish_event.py your-file.json`.
3. Query notifications for the new booking: status becomes `FAILED`. Tickets remain `ISSUED`.
4. Set `MOCK_NOTIFICATION_FAIL=false` and recreate the app again.
5. As ADMIN, POST the retry endpoint using the existing notification ID. Query until `SENT`; `attemptCount` increases and no new ticket/notification is created.

Mock failure is a local demonstration switch, not a payment/booking failure. In mock mode there are no actual delivery side effects and no contact addresses are required.

## Team integration: agree before merging

Member 2 must publish `booking.confirmed.v1` **after** verified payment and confirmed seats, keyed by booking ID. See `docs/INTEGRATION.md` for the precise JSON contract, ownership extension, JWT claims, and failure handling. Use the same customer ID representation in JWT `sub` and event `payload.customerId`.

Use environment variables for credentials. The provided broker is a single-node local PLAINTEXT broker, with host ports bound to loopback; it is not a production deployment. Team infrastructure must enforce producer/topic access and appropriate transport security. Shared HMAC JWT verification follows the blueprint's `JWT_SECRET`; all holders can sign tokens, so coordinate secret access or switch to public-key verification before broader deployment. The local token helper is not a login endpoint.

## Read the implementation in this order

1. `messaging/BookingConfirmedEvent.java`: incoming DTO and validation.
2. `ticket/TicketIssuanceService.java`: the transaction, booking lock, fingerprints, tickets, inbox.
3. `messaging/BookingConfirmedListener.java` and `config/KafkaConfig.java`: ingestion, retries, DLT.
4. `notification/NotificationWorker.java` and `SmtpEmailSender.java`: mock or real SMTP delivery.
5. `ticket/TicketQueries.java` and `TicketController.java`: reads, pagination, ownership, retry.
6. `config/SecurityConfig.java`: HS256 JWT validation and role mapping.
7. `src/test/`: executable examples and failure cases.

Persistence deliberately uses Spring JDBC instead of JPA so the MySQL upsert, row locks, and transactional boundaries are visible. Flyway owns schema changes. There are no cross-service database reads or foreign keys.
