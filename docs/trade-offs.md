# Trade-offs

Conscious shortcuts in the current scope. Each one is something to revisit before production.

## Messaging

### Retry backoff has no jitter
- **Where:** `KafkaConfig.kafkaErrorHandler`
- **Decision:** Plain exponential backoff (2s, ×2, max 3 attempts).
- **Why:** Spring's `ExponentialBackOff` does not support jitter natively; adding it requires a custom `BackOff` implementation.
- **Risk:** Synchronized retries from many consumers under a shared outage create a retry storm against the broker.
- **Production fix:** Custom `BackOff` with random jitter (e.g. ±25% of the next interval).

### Outbox publisher error classification is coarse
- **Where:** `OutboxKafkaPublisher.isNonRetryable`
- **Decision:** Treat `IllegalArgumentException`, `IllegalStateException`, and `SerializationException` as non-retryable; everything else is retryable and stops the batch to preserve order.
- **Why:** Quick, readable rule that catches the obvious data/serialization issues.
- **Risk:** A genuinely transient failure mistyped as transient (e.g., a wrapped serialization issue) could stall the queue at the head of the line.
- **Production fix:** Map a specific allow-list of exception types and root causes; emit a metric per classification so misclassification is visible.

### Outbox publisher does not claim rows
- **Where:** `OutboxKafkaPublisher.flush`
- **Decision:** Pending rows are read with a plain query and published; no row-level claim.
- **Why:** Single-instance assumption keeps the publisher simple and ordering trivial.
- **Risk:** Multi-instance deployment will publish the same row from multiple pods. Consumers stay correct because they are idempotent (ADR-002), but broker traffic doubles.
- **Production fix:** `SELECT ... FOR UPDATE SKIP LOCKED` per batch, or single-publisher leader election (e.g. ShedLock).

## Data

### No hard delete for products
- **Where:** `ProductController` exposes only `POST /products/{id}/takedown`; no `DELETE /products/{id}`.
- **Decision:** Products are taken down, never destroyed. The previous `DELETE` alias was removed.
- **Why:** Once a product has been published, it has produced events that downstream consumers (search index, cache, notification) reacted to. Hard delete would create silent inconsistency in those systems with no compensating event.
- **Risk:** Callers that expect "really gone" semantics will be surprised.
- **Production fix:** If a true delete is ever needed, add a `ProductDeleted` event with consumer-side compensation rather than a silent DB delete.

## API

### Metadata update is `PUT`, artist reassign is a separate `PUT`
- **Where:** `ProductController.update`, `ProductController.reassignArtist`
- **Decision:** Two endpoints — `PUT /products/{id}` (full metadata replace) and `PUT /products/{id}/artist` (reassign) — instead of one `PATCH /products/{id}` covering both.
- **Why:** PATCH would force "missing vs explicit null" handling on every field (needs `JsonNullable` or `Optional` wrappers), break `@NotBlank`-style validation, push conditional setters into the aggregate, and merge two distinct outbox concerns into one branchy service method.
- **Risk:** Clients that need to change metadata + artist atomically must send two requests.
- **Production fix:** If atomic multi-field updates become a real client need, add `PATCH /products/{id}` *alongside* the existing PUTs rather than replacing them.

### No artist search endpoint
- **Where:** `ArtistController`
- **Decision:** No `/artists?query=...` endpoint.
- **Why:** Doing search well needs an explicit query strategy (LIKE vs full-text vs ElasticSearch), index design, and pagination ordering decisions. None of those are interesting at this scope.
- **Production fix:** Decide read-model strategy first (DB full-text vs external search engine), then add the endpoint.

## Scope

The bigger "where this service would grow into" picture — `Delivery` aggregate, validation pipeline, scheduling, provider fan-out — is in [Domain Evolution](domain-evolution.md). It is intentionally a doc, not code.
