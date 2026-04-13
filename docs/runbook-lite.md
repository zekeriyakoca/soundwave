# Runbook Lite

## Scope
First-response guide for outbox publishing health.

## Alerts
| Alert | Condition | Severity | First Action |
|---|---|---|---|
| `OutboxPendingBacklogHigh` | `outbox_events_pending > 100` for 5m | warning | Inspect the oldest pending row — ordered publish halts on the first retryable failure. Verify broker/topic reachability only if no single row is stuck. |
| `OutboxFailedEventsDetected` | `outbox_events_failed > 0` for 2m | critical | Read `failure_reason` on failed rows; classify data (payload/mapping) vs infra (serialization/config) before deciding replay. |
| `OutboxDltEventsDetected` | `increase(outbox_dlt_events_total[10m]) > 0` for 1m | warning | Pull a DLT record sample and read the `kafka_exception*` headers to classify the failure: non-retryable (schema/validation mismatch, unknown event type) vs retries-exhausted under transient broker/consumer instability. |

## Quick Checks
1. `GET /actuator/health`
2. `GET /actuator/metrics/outbox.events.pending`
3. `GET /actuator/metrics/outbox.events.failed`
4. Search structured logs in Seq (http://localhost:8090) by `eventId`, `eventType`, or `correlation_id`

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
