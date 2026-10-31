# Real email delivery

## 1. Upgrade the existing project

Copy the updated source/configuration/scripts into your existing project folder. Preserve your local `.env`, `.tools/`, and MySQL volume. Do not delete the database or edit an applied V1 migration. Flyway runs `V2__smtp_delivery.sql` automatically at application startup.

## 2. Configure your SMTP relay

Get the SMTP settings and authorized sender from your email provider or IT. Add these fields to the existing `.env` (replace examples with real values):

```dotenv
NOTIFICATION_MODE=smtp
MAIL_FROM=tickets@your-domain.example
SMTP_HOST=smtp.your-provider.example
SMTP_PORT=587
SMTP_USERNAME=your-smtp-username
SMTP_PASSWORD=your-smtp-password-or-provider-app-password
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_STARTTLS_REQUIRED=true
SMTP_SSL_ENABLED=false
```

These are example values, not working credentials. This implementation supports SMTP username/password or an unauthenticated approved relay, not OAuth token acquisition. Providers that require OAuth need a corresponding authentication adapter. Use your provider's SMTP credential/app-password mechanism when allowed. Never paste SMTP passwords into chat or commit `.env`.

For implicit TLS on port 465, use `SMTP_PORT=465`, `SMTP_SSL_ENABLED=true`, `SMTP_STARTTLS=false`, `SMTP_STARTTLS_REQUIRED=false`. Keep certificate and server-name checks enabled. An approved unauthenticated relay may require `SMTP_AUTH=false`. STARTTLS/TLS should remain enabled for real external email unless IT explicitly provides a trusted local relay arrangement. Plaintext settings below are for the local capture service only.

Then rebuild/recreate:

```bash
docker compose up -d --build
```

Spring Boot alone does not load `.env`. For an IDE or `bash mvn-local spring-boot:run`, set the same variables in the run configuration or export them into the current shell. `localhost` inside the app container means the container itself; use a reachable relay hostname or `host.docker.internal` for a host relay supported by your runtime.

## 3. Send a fresh demo booking

From the project directory, use a mailbox you are authorized to test:

```bash
python3 scripts/publish_email_demo.py --email your-address@your-domain.example
```

This publishes a **new** booking ID with two seats and an explicit customerEmail. Expect one email per ticket (two emails), each with plain-text and HTML content, booking ID, event ID, seat ID, ticket code, and issued time. It uses local Kafka as a test producer; the real Booking service must publish the same contract.

Generate your customer token and inspect the booking ID printed by the script:

```bash
TOKEN=$(python3 scripts/dev_token.py)
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8084/api/v1/notifications/by-booking/REPLACE_WITH_PRINTED_BOOKING_ID \
  | python3 -m json.tool
```

Expected per notification: `deliveryMode: SMTP`, `status: SENT`, `attemptCount: 1`, `deliveryUncertain: false`, and a non-null `sentAt`. Check the recipient mailbox/spam folder and your provider's delivery logs. SENT records SMTP acceptance, not guaranteed final delivery.

## 4. Recipient contract / existing data

The Booking event payload adds:

```json
{"customerId":"customer-001","customerEmail":"your-address@your-domain.example","targetEventId":101,"confirmedAt":"2026-10-29T12:00:00Z","items":[{"seatId":501}]}
```

All existing envelope fields remain required. Missing email is allowed for compatibility: tickets are still issued, while SMTP notifications fail with a safe explanation. A malformed email rejects the event as invalid. No address is obtained from a user-controlled ticket GET request.

Recipients and delivery mode are captured on first issuance and do not change on replay. Existing SENT mock records will stay mock; they are not emailed retroactively. Use a fresh booking for the demo. The current API has no address-correction or resend-SENT endpoint.

## 5. Preview using a local SMTP inbox

To test the real SMTP protocol without sending external mail:

```bash
docker compose -f compose.yml -f compose.mailpit.yml up -d --build
```

Open http://localhost:8025. The local capture server accepts mail without TLS/authentication on its private Compose network; its UI is bound to host loopback. The app uses `smtp` delivery in this override. Set `NOTIFICATION_MODE=smtp` in `.env` for the demo helper's mode check (the override already sets it for the container), then publish:

```bash
python3 scripts/publish_email_demo.py --email demo@example.test
```

Two messages should appear in the local inbox. This proves SMTP submission/rendering, not external provider delivery. Use the same two `-f` options for commands that manage this stack. The base Compose file is used by the publisher to reach the same Kafka service.

## 6. Failure and retry

Read `lastError`, `deliveryMode`, `deliveryUncertain`, and `attemptCount` via the authorized notification API.

- FAILED with `deliveryUncertain=false`: fix configuration before ADMIN retries.
- FAILED with `deliveryUncertain=true`: the relay may have accepted the message. Check provider logs before any resend.
- PENDING: queued or claimed by a worker. A process crash leaves an uncertain FAILED row after the two-minute claim lease expires and a worker runs.

Normal ADMIN retry:

```bash
ADMIN_TOKEN=$(python3 scripts/dev_token.py --role ADMIN)
curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8084/api/v1/notifications/retry/REPLACE_WITH_NOTIFICATION_ID
```

Only after checking an uncertain outcome, deliberately request a resend with the same URL plus `?allowDuplicate=true`. This may produce a duplicate email. No automatic retry is performed for SMTP exceptions or expired claims. Ticket rows are never rolled back because mail fails.

## 7. Tests

`bash mvn-local test` includes a loopback SMTP receiver test. It checks actual JavaMail submission and MIME content, two-ticket delivery, no resend on Kafka replay, missing-recipient handling, a server that accepts DATA then drops the acknowledgment, expired claims, exclusive claims, and header validation. No external email is sent by these automated tests. MySQL/Testcontainers and Compose/Mailpit checks require Docker.
