# Member 4 integration contract

## Booking → Ticket

Topic and event type: `booking.confirmed.v1`. Kafka key: exactly the `bookingId` string. Consumer group: `ticket-notification-service` (all instances share it). Default partitions: 3; DLT must have the same partition count. Initial consumption uses `earliest`; existing consumer offsets take precedence.

`examples/booking-confirmed.json` is executable sample data. All fields shown there are required. Envelope `eventId` is a unique message ID. `payload.targetEventId` is the catalog event ID. `schemaVersion` must be integer 1. `payload.customerId` is a proposed addition to the PDF's minimum event fields: Member 2 must include it for this service's local ownership checks. Do not silently infer ownership from an HTTP caller or unverified ID.

`bookingId`, `eventId`, `customerId` are nonblank strings up to 100 characters. `correlationId` allows 1–100 characters from letters, digits, `. _ : -`. `targetEventId` and seat IDs are positive 64-bit integers. Items contain 1–100 distinct seat IDs. Timestamps are ISO-8601 instants. Unknown JSON fields are tolerated for additive changes; incompatible versions are rejected.

Booking must only emit confirmation after verified payment **and** inventory confirmation. Failed/pending bookings do not produce tickets. No payment or inventory event is consumed directly by this service.

## Atomicity and idempotency

One MySQL transaction: acquire/upsert the booking_issuances row, SELECT FOR UPDATE, compare canonical payload fingerprint, create tickets and pending notifications once, insert inbox row, commit.

- `inbox_messages.message_id` deduplicates exact message replay.
- `booking_issuances.booking_id` serializes concurrent confirmations for the same booking.
- A SHA-256 hash of structured `[bookingId,customerId,targetEventId,sortedSeatIds]` rejects changed ownership/event/seat sets under the same booking. Delivery timestamps and correlation metadata do not change the business identity.
- `tickets(booking_id,seat_id)` and unique `ticket_code` are database backstops.
- Notification operation key is `ticket:{ticketId}:EMAIL:issued`.
- Same logical confirmation with a new message ID records the inbox entry without reissuing.
- Same message ID reused across bookings is a contract error; writes roll back.

The booking_issuances table is an intentional addition to the blueprint. It prevents two differently shaped confirmation messages from quietly adding tickets under one booking, and provides a serialization lock even before the first ticket exists.

This service trusts the authorized Booking producer's finality. It cannot independently prove payment or seat confirmation from a Kafka message. Broker credentials/ACLs belong to shared infrastructure. Kafka records have no JWT authentication at this application layer.

## Offsets, retries and dead letters

`enable.auto.commit=false`, record acknowledgment. The listener calls a separate Spring service; the database transaction commits before the listener returns. A crash after MySQL commit but before Kafka offset commit causes harmless redelivery. Kafka and MySQL are **not** one atomic distributed transaction.

Malformed JSON, wrong Kafka key, invalid DTO, unsupported schema, and conflicting confirmations go directly to `booking.confirmed.v1.DLT`. Temporary exceptions retry three times at one-second intervals, then go to DLT. DLT publish must be acknowledged; a failed DLT send throws and does not intentionally skip the original record. Default Spring Kafka DLT headers preserve source/error diagnostics. Operators must monitor DLT and remediate/replay. An exhausted DB failure may require replay after recovery; it is not automatically retried forever.

Use the same booking key for ordering. Changing the partition count can change key placement; coordinate migrations. DLT handling means later records may proceed after an earlier failed record is quarantined. Replays must preserve original message IDs unless intentionally creating a new event.

There is no outgoing business event in the initial scope, so no outbox is required here. Add a transactional outbox if introducing a durable `tickets.issued` downstream contract.

## JWT contract with Member 2

Current implementation: HS256; minimum 32 UTF-8 secret bytes; raw string secret, not base64-decoded. Configure identical `JWT_SECRET` and `JWT_ISSUER`. Required claims:

```json
{"sub":"customer-001","iss":"smart-event-booking","aud":"ticket-api","role":"CUSTOMER","iat":1792335600,"exp":1792339200}
```

The example timestamps are illustrative; generate current tokens. Audience may be a string or a JWT audience array containing `ticket-api`. A nonblank subject and expiry are required. Signature, issuer, time validity, and audience are checked. ADMIN is exactly `role: "ADMIN"`; any other role maps to ordinary customer access. Signing algorithms or a `roles[]` claim are not auto-detected. Coordinate changes explicitly. Spring timestamp validation includes its standard clock-skew tolerance.

Customers may read only tickets/notifications whose stored customer ID equals JWT `sub`. ADMIN may read any booking and retry failed notifications. No public ticket creation endpoint exists. UUIDs are not authorization.

## Notifications

Both MOCK and SMTP delivery are implemented. See `EMAIL_SETUP.md` for operational setup. Booking may add the optional, validated `payload.customerEmail` string (max 254 characters, no CR/LF). It is required for real SMTP delivery but not for ticket issuance. In SMTP mode, a missing email produces a FAILED notification and retains the ticket. SMTP recipients are immutable snapshots taken when a ticket is first issued; replay cannot redirect an existing ticket email. The existing canonical booking fingerprint intentionally excludes the added email field, preserving compatibility with already-issued bookings. This means email-only changes on replay are ignored rather than updating the recipient.

One row represents one logical notification per ticket, with cumulative attempt_count, recent error, recipient_email, delivery_mode, claim_token, lease_until, delivery_uncertain and sent_at. V2 is additive and never changes the V1 checksum. Existing rows default to MOCK, so enabling SMTP does not turn old mock records into outgoing email. SMS is still an extension.

The worker atomically claims one PENDING row using FOR UPDATE SKIP LOCKED, increments its attempt count, and commits. It sends outside the database transaction, then conditionally records completion using the claim token. A two-minute lease protects against abandoned claims. Expired claims are marked FAILED with deliveryUncertain=true; they are not automatically re-sent. This prevents blind duplicate sends after a process dies following SMTP acceptance. A stale worker cannot overwrite the recovered row.

SENT in SMTP mode means the configured SMTP relay accepted submission, not that the final mailbox received or read it. SMTP errors that could occur after acceptance are conservatively FAILED/uncertain. Authentication and local preparation errors are known failures. The worker logs only IDs, mode, status, and uncertainty, not addresses, credentials, message bodies or raw SMTP exceptions.

Manual retry accepts FAILED → PENDING. For uncertain delivery, the ADMIN must first inspect provider logs and explicitly pass `?allowDuplicate=true`; this may send a second email. A repeat retry after acceptance returns 409. Stable Message-ID and X-Notification-ID aid tracing but do not guarantee provider deduplication. There is no automatic SMTP retry loop, provider-delivery webhook, bounce tracking, or exactly-once email guarantee. Notification failure never cancels tickets.

## Business rules / traceability

| Rule | Implementation | Evidence |
| --- | --- | --- |
| BR-T1: exactly one ticket per booking/seat | TicketIssuanceService + database UNIQUE | duplicate and concurrency tests |
| BR-T2: all tickets/notifications/inbox commit together | issuance transaction | rollbackThenRetryWorks |
| BR-T3: confirmed booking payload cannot change | canonical hash + booking row lock | conflictingBookingAndReusedMessageAreRejectedAtomically |
| BR-T4: customers can read only their own tickets/notifications | TicketQueries + SecurityConfig | ownership/JWT tests |
| BR-T5: notification failure never revokes a ticket | separate worker transaction | mockFailureRetryAndSuccessPreserveTickets |
| BR-T6: failed notification retry reuses the logical operation | retry state guard + unique key | notification and HTTP retry tests |

## Health / configuration

`GET /health` is public and reveals only status. Database and Kafka checks participate in overall health; dependency failure returns 503. No passwords, tokens, or event bodies are intentionally logged. HTTP correlation IDs are returned in `X-Correlation-ID`; workflow correlation IDs enter structured logs.

The DB URL defaults to UTC connection/session time. Never add cross-service foreign keys, direct queries of booking_db, or shared MySQL credentials.
