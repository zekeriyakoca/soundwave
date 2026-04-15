# ADR-001: Transactional Outbox

## Context
- Product/artist updates must create reliable events.
- DB commit and Kafka publish are not one atomic transaction.

## Decision
- Write domain change and outbox row in the same DB transaction.
- Publish pending rows after commit.
- Read pending rows ordered by `created_at, id`.
- Stop current batch on transient failure to keep order safe.

## Alternatives Rejected
- **Direct Kafka publish in service method**
  - Rejected: DB/Kafka can diverge on partial failure.
- **CDC outbox (Debezium)**
  - Rejected: extra infrastructure complexity outweighs the benefit at the current scale.

## Consequences
- Consistency model is simple and robust for the current scope.
- Multi-node duplicate publish is still possible without DB-level claiming.
- Transient failure halts the whole batch, not just the affected aggregate.

## Success Criteria
- Committed publish/takedown actions always produce an outbox row.
- `outbox.events.pending` stays near zero in normal flow.
