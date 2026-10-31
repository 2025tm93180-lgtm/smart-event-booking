# Latest validation: SMTP email update

`mvn verify` completed successfully with Java 21.

| Suite | Passed | Skipped | Coverage |
| --- | --- | --- | --- |
| ServiceIntegrationTest | 10 | 0 | Issuance, transaction rollback, duplicates, concurrency, mock retry, HTTP ownership and signed JWT validation |
| KafkaFlowTest | 1 | 0 | Embedded KRaft Kafka, consumer, duplicate messages, DLT, database/Kafka health |
| SmtpDeliveryTest | 6 | 0 | Actual loopback SMTP/MIME, two emails per two-seat booking, no resend on replay, missing email, lost SMTP ACK/explicit retry, abandoned claims, concurrent claims, header validation |
| MySqlConcurrencyTest | 0 | 1 | Docker unavailable; real MySQL migration/locking test not executed here |

Total: **17 passed, 0 failed, 1 skipped**. Executable Spring Boot JAR built successfully. The source download excludes generated JARs and local tool binaries.

SMTP tests used a loopback receiver and did not send mail externally. Actual mailbox delivery requires the user's SMTP credentials and recipient. Mailpit/Compose and real MySQL smoke checks were not executed in this environment. The Mailpit release tag used in the optional Compose override was verified against the publisher's release metadata.

Python syntax, configuration XML/YAML, OpenAPI copies, and unchanged V1 migration bytes were checked. V2 adds mail-delivery columns; it does not edit V1 or delete data.

The previously verified quoted Surefire argument and folder-local macOS Java/Maven installer remain included. The SMTP update was verified in the regular workspace path; the earlier spaces-in-path regression evidence remains under docs/evidence.
