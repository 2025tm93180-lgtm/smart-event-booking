# Seat Inventory API (initial implementation)

The inventory implementation is part of the existing Event & Inventory Spring Boot service. It uses the service's configured MySQL datasource and Hibernate schema updates.

## Create seats for an event
`POST /api/events/{eventId}/seats`

```json
{
  "seatIds": ["A1", "A2", "A3"],
  "price": 499.00
}
```

Seat IDs must be unique within an event. The number of created seats cannot exceed the event's configured capacity. This first version applies one price to all seats in a single request.

## List seats
`GET /api/events/{eventId}/seats`

Each seat returns its status (`AVAILABLE`, `HELD`, or `RESERVED`) and price.

## Hold seats for 10 minutes
`POST /api/reservations`

```json
{
  "bookingId": "booking-123",
  "eventId": "EVE-12345",
  "seatIds": ["A1", "A2"]
}
```

The server calculates the total from stored seat prices. All requested seats are locked and checked in one transaction. If any seat is unavailable, the request fails with HTTP 409 and no new reservation is created. The response includes `id`, `totalAmount`, `status`, and `expiresAt`.

A booking ID is used as an idempotency identifier: repeating the same request returns the existing reservation; reusing the ID for different seats/event returns HTTP 409.

## Confirm or release
- `POST /api/reservations/{reservationId}/confirm`
- `POST /api/reservations/{reservationId}/release`
- `GET /api/reservations/{reservationId}`

Confirmation changes held seats to `RESERVED`; release changes held seats back to `AVAILABLE`. A scheduled task scans expired holds every 30 seconds by default (`inventory.expiry-scan-ms` overrides this). Expired holds return seats to `AVAILABLE`.

## Current scope / next steps
This is the REST + database inventory foundation. Kafka commands/results, durable outbox/inbox handling, booking-service integration, and concurrency/expiry integration tests still need to be added before the full Saga is complete. A production deployment should use Flyway migrations instead of relying only on `ddl-auto: update`.
