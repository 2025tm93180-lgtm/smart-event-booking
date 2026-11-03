# Infrastructure

Infrastructure shared by the local development stack is defined in the repository-root `docker-compose.yml`.

## MySQL initialization

`mysql/init/01-databases.sql` creates the databases used by booking, event, payment, and ticket services when MySQL initializes a new data directory. The root Compose file mounts this directory into MySQL's initialization path and stores database files in the `mysql-data` volume.

Initialization scripts run only on the first start with an empty MySQL volume. To re-run initialization from scratch, remove the volume with `docker compose down -v` from the repository root. This deletes all persisted local database contents.

## Kafka

The root Compose stack runs a single-node Kafka broker for local development. Containers connect to `kafka:19092`; tools running on the host connect to `localhost:9092`. The broker uses plaintext and is not configured for production use.

## Start shared dependencies

From the repository root:

```bash
docker compose up -d mysql kafka
docker compose ps
```

See the root [README](../README.md) for the full stack and service-specific guides.
