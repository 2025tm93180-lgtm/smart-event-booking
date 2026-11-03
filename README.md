# Smart Event Ticket Booking System

A local development project for an event booking platform built as a set of Spring Boot services, a React UI prototype, MySQL, and Kafka. The backend services use separate databases and exchange workflow events through Kafka. Booking and payment flows are designed around a Saga and outbox/inbox patterns. The UI is currently a standalone prototype with mock data; it does not call the backend services yet.

## Contents

- [Repository layout](#repository-layout)
- [Clone the project](#clone-the-project)
- [Run everything with Docker Compose](#run-everything-with-docker-compose)
- [Work on an individual service](#work-on-an-individual-service)
- [Development workflow](#development-workflow)
- [Useful Compose commands](#useful-compose-commands)
- [Service documentation](#service-documentation)
- [Configuration and local data](#configuration-and-local-data)

## Repository layout

```text
booking-service/      Booking API, persistence, Saga workflow and Kafka integration
event-service/        Event, venue, seat and reservation APIs
payment-service/      Payment records, Razorpay checkout and Kafka integration
ticket-service/       Ticket issuance, notification handling and Kafka consumer
ui/                   React + TypeScript customer portal prototype
infrastructure/       MySQL initialization and infrastructure notes
docs/                 Project-level documentation
docker-compose.yml    Full local development stack
```

Each backend folder is an independent Maven project with its own `pom.xml` and `Dockerfile`. The UI has its own `package.json` and Dockerfile.

## Clone the project

```bash
git clone <repository-url>
cd smart-event-booking
```

Replace `<repository-url>` with the URL for your Git repository. Check out the branch you intend to work on before making changes:

```bash
git switch <branch-name>
```

## Run everything with Docker Compose

### Requirements

- Docker Desktop or Docker Engine with the Docker Compose v2 plugin
- Git to clone the repository

Run Compose commands from the repository root (the directory containing `docker-compose.yml`). No host Java, Maven, Node, or npm installation is needed to run the full stack.

### Optional local configuration

The Compose file has local-development defaults. For local Razorpay testing or custom secrets, create a `.env` file next to `docker-compose.yml`:

```dotenv
MYSQL_ROOT_PASSWORD=choose-a-local-password
RAZORPAY_KEY_ID=rzp_test_your_key_id
RAZORPAY_KEY_SECRET=your_test_key_secret
JWT_SECRET=replace-with-a-long-random-secret-value
```

Compose reads this file automatically. Razorpay credentials are required for Razorpay order creation and verification; the payment service can start without them. Use Razorpay test credentials for development. The default values are for local use only and must not be used for a publicly accessible deployment.

### Build and start

```bash
docker compose up --build
```

Compose builds the backend and UI images, starts MySQL and Kafka, waits for their health checks, then starts the services that depend on them. The first build can take several minutes. Leave the terminal open to watch logs. To start in the background:

```bash
docker compose up --build -d
```

Check that containers are running:

```bash
docker compose ps
```

### Local addresses

| Component | Address | Container name |
| --- | --- | --- |
| UI prototype | http://localhost:3000 | `smart-event-ui` |
| Booking service | http://localhost:8081 | `smart-event-booking-service` |
| Ticket service | http://localhost:8084 | `smart-event-ticket-service` |
| Event service | http://localhost:9091 | `smart-event-event-service` |
| Payment service | http://localhost:9090 | `smart-event-payment-service` |
| MySQL | `localhost:3307` | `smart-event-mysql` |
| Kafka for host clients | `localhost:9092` | `smart-event-kafka` |

The services communicate over the Compose network using `mysql:3306` and `kafka:19092`. Host tools connect to MySQL through `localhost:3307` and Kafka through `localhost:9092`. Containers should use the internal addresses; `localhost` inside a container refers to that container itself.

The UI currently uses local mock data, so its screens are a prototype and do not exercise the backend APIs. Consult each service README for endpoints and service-specific behavior.

### Stop the stack

Stop and remove the containers while preserving the database volume:

```bash
docker compose down
```

To start the stack again, use `docker compose up -d` (or add `--build` after changing code or Docker configuration).

To delete the saved MySQL data as well:

```bash
docker compose down -v
```

This permanently deletes the Compose database volume. MySQL initialization scripts run only when MySQL initializes a fresh data directory.

## Work on an individual service

To rebuild and restart one service after changing its code:

```bash
docker compose up -d --build booking-service
docker compose logs -f booking-service
```

Replace `booking-service` with `event-service`, `payment-service`, `ticket-service`, or `ui`. Shared dependencies can be started without the other app containers:

```bash
docker compose up -d mysql kafka
```

Each backend can also be run from its own directory using Maven, and the UI can be run with Vite. See the service READMEs for prerequisites, commands, tests, and environment variables. When running a backend directly on the host, use `localhost` for MySQL/Kafka and configure its service-specific environment variables as described in its README.

## Development workflow

1. Read this guide and the relevant service README before editing.
2. Start the full stack with Compose, or start just MySQL and Kafka for host-based development.
3. Make changes within the service or UI that owns the behavior. Keep API and Kafka contract changes coordinated with services that consume them.
4. Build or run the relevant service using the instructions in its README.
5. Review the changes with `git status` and `git diff`, then commit your work on your branch.

Database schema initialization for the Compose stack is in `infrastructure/mysql/init/`. The databases are created on the first start of a new MySQL volume. Existing service schema migrations or ORM settings are owned by the service itself.

## Useful Compose commands

```bash
# Show containers and health
docker compose ps

# Follow all logs
docker compose logs -f

# Follow one service
docker compose logs -f payment-service

# Restart a service
docker compose restart ticket-service

# Rebuild and recreate a changed service
docker compose up -d --build event-service

# Stop containers, retaining database data
docker compose down
```

## Service documentation

- [Booking service](booking-service/README.md)
- [Event service](event-service/README.md)
- [Payment service](payment-service/README.md)
- [Ticket and notification service](ticket-service/README.md)
- [UI prototype](ui/README.md)
- [Infrastructure](infrastructure/README.md)
- [Project docs](docs/README.md)

## Configuration and local data

The root Compose stack persists MySQL files in the `mysql-data` Docker volume. The init SQL creates the separate databases used by the services. To see the resolved Compose configuration without starting containers, run:

```bash
docker compose config
```

The main Compose file binds published ports to loopback so the development services are available only from the local machine. Kafka uses PLAINTEXT for this local setup. This Compose stack is intended for development and demonstration, not production deployment.
