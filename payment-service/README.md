# Payment service

Spring Boot service for payment records, Razorpay checkout and verification, and Kafka-based booking/payment events. It stores payment state in the `payment_service` MySQL database and listens on port `9090`.

## Run with the full stack

From the repository root, configure Razorpay test credentials in the root `.env` if you want to create orders, then run:

```bash
docker compose up -d --build payment-service
docker compose logs -f payment-service
```

Compose also starts its required MySQL and Kafka services. The payment API is available at `http://localhost:9090`.

## Run the standalone payment Compose stack

From `payment-service/`, copy `.env.example` to `.env`, adjust secrets, then run:

```bash
docker compose up -d --build
docker compose logs -f payment-service
```

## Configuration

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`: MySQL connection.
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka brokers (`kafka:19092` inside Compose).
- `PAYMENT_EVENTS_TOPIC` and `BOOKING_EVENTS_TOPIC`: event topics.
- `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET`: Razorpay credentials. Supply test credentials for local testing and keep secrets out of source control.

The service's YAML configuration contains local defaults. The full-stack Compose configuration injects database, Kafka, and Razorpay environment variables.

## Main API routes

- `GET /health`: health endpoint.
- `POST /api/create-order`: create a Razorpay order.
- `POST /api/verify-payment`: verify a Razorpay signature.
- `GET /api/config`: return the configured public Razorpay key ID.
- `GET /api/v1/payments/{paymentId}` and `/api/v1/payments/by-booking/{bookingId}`: retrieve payment records.
- `POST /api/v1/payment`: create a payment record.
- `POST /api/v1/payments/{paymentId}/simulate-decline`: development payment-decline path.

See the controller classes and DTOs for request and response shapes.

## Build

```bash
mvn clean package
```

The Dockerfile builds the JAR with Maven and runs it in a Java 17 runtime image.
