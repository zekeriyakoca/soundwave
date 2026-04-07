# DLT Recovery Plan

## Why
DLT is not a trash bin. It is a quarantine for messages that failed normal processing.

This is a common industry pattern: keep failed messages, fix root cause, replay in a controlled way.

## Current Behavior
- Kafka error handler sends exhausted/non-retryable records to `*.dlt`.
- `DeadLetterLogConsumer` logs the record and increments `outbox.dlt.events`.

## Simple Target Plan
1. Store DLT records in a small `dlt_events` table (`id`, `topic`, `partition`, `offset`, `payload`, `status`, `created_at`).
2. Expose a controlled replay script/endpoint to replay selected records by ID.
3. After successful replay, mark status `REPLAYED`; keep audit trail.
4. Keep runbook steps for root-cause fix before replay.

## Rewind Option
Kafka offset rewind is possible, but should be a last resort.
Reason: it can replay many unrelated messages; use only when idempotency is trusted.

## Scope Note
The current implementation keeps DLT handling at log + metric level. The plan above is the next practical step.
