# Event Contract Governance

## Contract Source
- Envelope: `OutboxEnvelope(eventId, eventType, payload)`
- Event type rules: `CatalogEventSchema`

## Compatibility Policy
- Existing event type names are immutable.
- Payload changes are additive-only.
- Breaking change requires a new event type.

## Validation
- Consumer validates envelope/event type before idempotency/process logic.
- Payload field validation is handled in consumer/domain logic in this scope.

## Review Rule
- Contract-related changes must include/adjust event contract tests.
