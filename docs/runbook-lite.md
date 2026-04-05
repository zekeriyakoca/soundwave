# Runbook Lite

## Scope
First-response guide for outbox publishing health.

## Alerts
- `OutboxPendingBacklogHigh`
- `OutboxFailedEventsDetected`
- `OutboxDltEventsDetected`

## Owner Chain
| Alert | Owner | First Action | Escalation |
|---|---|---|---|
| Pending backlog | Backend on-call | Check broker reachability + pending trend | Platform on-call |
| Failed rows | Backend on-call | Inspect `failure_reason`, classify data vs infra | Domain owner |
| DLT growth | Backend on-call | Inspect sample payload + cause | Consumer owner |

## Quick Checks
1. `GET /actuator/health`
2. `GET /actuator/metrics/outbox.events.pending`
3. `GET /actuator/metrics/outbox.events.failed`
4. Check logs by `eventId` / `eventType`

## Recovery Hints
- **Pending rising**
  - Possible reasons:
    - broker unreachable / topic misconfiguration
    - oldest retryable row keeps failing, and ordered publish loop stops at that row
  - Action:
    - verify broker/topic
    - inspect first pending outbox row and publish error
- **Failed rows present**
  - Possible reasons:
    - non-retryable serialization/data issue
    - unknown aggregate/event mapping
  - Action:
    - inspect `failure_reason`
    - fix payload/config/code and replay affected events
- **DLT increasing**:
  - Possible reasons:
    - consumer payload/validation mismatch
    - non-retryable exception classification
    - retries exhausted under broker instability
  - Action:
    - inspect DLT record sample
    - classify retryable vs non-retryable
    - fix root cause
    - replay DLT records with a controlled script/process

## DLT Handling Policy
- DLT is a quarantine queue, not a delete queue.
- Keep DLT payloads available for replay/recovery.
- Use [DLT Recovery Plan](dlt-recovery-plan.md) as the target implementation path.

## Close Condition
- Metric returns below threshold.
- Root cause and owner are documented.
