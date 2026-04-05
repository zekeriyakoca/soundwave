# ADR-002: Idempotent Consumer

## Context
- Kafka delivery is at-least-once.
- Combined with the outbox publisher (which can also retry on transient failure), the same event ID can land on a consumer more than once.
- Duplicate deliveries must not trigger duplicate side effects (cache evict, search index update, notification, etc.).
- Exactly-once end-to-end is possible (Kafka transactions + transactional producers/consumers) but pulls in significant operational and code complexity.

## Decision
- Accept at-least-once delivery as the system's default guarantee.
- Make every consumer idempotent by storing `(event_id, consumer_group)` in `processed_events`.
- Insert first, then process. If insert conflicts, treat as duplicate and skip.
- Each consumer group has its own dedup row, so consumers are independent.

## Why insert-then-process
Both the dedup insert and `process()` run in the same transaction.

- `process()` fails → dedup row rolls back too → next delivery retries cleanly.
- Reverse order would risk: side effect committed, dedup write lost, side effect replayed.

Caveat: only safe when the side effect is itself transactional or idempotent (cache evict, DB writes we own). Fire-and-forget side effects (e.g. third-party HTTP call) need a different pattern.

## Alternatives Rejected
- **In-memory dedup cache**
  - Rejected: restart/eviction can lose dedup history; not safe across instances.
- **Exactly-once end-to-end (Kafka transactions)**
  - Rejected: requires transactional producer + read-process-write consumer pattern, broker config tuning, and ties producer/consumer lifecycles. Too much complexity for the assignment scope and not needed once consumers are idempotent.
- **Producer-side dedup only**
  - Rejected: does not protect against consumer reprocessing after offset replay or rebalance.

## Consequences
- Clear, practical dedup behavior that survives restarts and scale-out.
- Slight write amplification: every successfully processed event also writes a `processed_events` row.
- `processed_events` grows unbounded. Retention is not implemented in this scope; production needs a periodic cleanup job (e.g. delete rows older than 30 days, or partition by month and drop old partitions). Window must exceed Kafka's longest possible replay window to stay safe.
- Current duplicate detection is broad (`DataIntegrityViolationException`) and should be narrowed to unique-constraint violations only.

## Success Criteria
- Replaying the same event ID does not repeat business side effects.
- A consumer crash mid-process does not leave the system in a state where the event is silently lost or doubly applied.
