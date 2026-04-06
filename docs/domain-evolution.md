# Domain Evolution (Phase 2)

Phase 1 (the current code) publishes a product and it becomes active immediately. Phase 2 turns publish into the entry point of a release pipeline: auto-validation, optional manual review, a release date the product waits on, and provider fan-out. Documented, not built.

## The pipeline

`Product` carries a `releaseDate` and a `providers` list. Publishing no longer makes the product live — it enters the pipeline:

1. **Publish** → `AUTO_VALIDATING`. Async auto-validation kicks off.
2. Auto-validation result → `READY_TO_RELEASE` (pass) or `FAILED_TO_AUTO_VERIFY` (fail).
3. **Manual review** (ops API) is valid from **either** `READY_TO_RELEASE` or `FAILED_TO_AUTO_VERIFY`, and stays valid on `READY_TO_RELEASE` until the release date is reached. Pass → `READY_TO_RELEASE`. Fail → `FAILED_TO_MANUAL_REVIEW`.
4. **Background job** scans `READY_TO_RELEASE` for products whose `releaseDate ≤ now` and transitions them to `RELEASED`.
5. `RELEASED` emits a `released` event carrying the product's provider list. Consumers handle the actual delivery.

### States and transitions

| From | Trigger | To |
|---|---|---|
| `DRAFT` | publish | `AUTO_VALIDATING` |
| `AUTO_VALIDATING` | auto-validate pass | `READY_TO_RELEASE` |
| `AUTO_VALIDATING` | auto-validate fail | `FAILED_TO_AUTO_VERIFY` |
| `READY_TO_RELEASE` / `FAILED_TO_AUTO_VERIFY` | review pass | `READY_TO_RELEASE` |
| `READY_TO_RELEASE` / `FAILED_TO_AUTO_VERIFY` | review fail | `FAILED_TO_MANUAL_REVIEW` |
| `READY_TO_RELEASE` | scheduler: `releaseDate` reached | `RELEASED` |
| `FAILED_TO_MANUAL_REVIEW` | edit + republish | `AUTO_VALIDATING` |

`AUTO_VALIDATING` is the only state name I had to coin (the others come from your wording). `FAILED_TO_MANUAL_REVIEW` is recoverable via republish — same code path as a fresh publish.

## Saga

Choreography saga over the existing outbox. Saga state lives on `Product.status`. Each step is its own consumer that listens for the previous event and emits the next. Idempotency is free via the existing `processed_events` inbox (ADR-002).

| Step | Trigger | Worker | Result |
|---|---|---|---|
| Publish | API call | `ProductService.publish` | `AUTO_VALIDATING`; emit `ProductPublished` |
| Auto-validate | `ProductPublished` | `AutoValidationConsumer` | `READY_TO_RELEASE` or `FAILED_TO_AUTO_VERIFY` |
| Manual review | API call (ops) | `ReviewService` | `READY_TO_RELEASE` or `FAILED_TO_MANUAL_REVIEW` |
| Release | scheduler tick | `ReleaseScheduler` | `RELEASED`; emit `released` |
| Deliver | `released` | provider consumer(s) | per-provider delivery |

Choreography over orchestration because the pipeline is linear and single-service — no central coordinator earns its weight. Failures just flip the status back to a recoverable state.

## Background job for release date

Polling worker, same shape as `OutboxKafkaPublisher` (`@Scheduled(fixedDelay)` + `AtomicBoolean` claim, see `OutboxKafkaPublisher.java:79-96`). Multi-instance safety via `SELECT ... FOR UPDATE SKIP LOCKED`, the same fix already noted in `trade-offs.md`. Zero new infra.

## Provider fan-out

The `released` event carries the provider list (copied from `Product.providers` so consumers don't query back).

| Shape | Pros | Cons |
|---|---|---|
| **A — One consumer per provider** | Independent failure domains, retry, scaling. Provider-specific code stays in one place per consumer. | N consumer groups to operate; every consumer reads every event. |
| **B — Single dispatcher consumer** | One group to operate, no wasted reads. | A slow or buggy provider stalls the others in the same dispatch loop. |

B is fine while there are one or two providers behaving similarly. A becomes worth it as soon as providers diverge in SLA or retry policy. The producer side is identical in both — the choice is local to the consumer side and reversible.

## Why this is documented, not built

The interesting parts of phase 2 are the saga shape, the scheduler, and the fan-out trade-off. Building any of them properly needs the new states wired through controllers, services, and tests plus at least one fake provider adapter — days of work where most of the diff is plumbing. The current scope already demonstrates outbox + idempotent consumers + cache invalidation; phase 2 reuses those patterns without changing them.
