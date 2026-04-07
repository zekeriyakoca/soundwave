# ADR-003: Event Envelope and Schema Guard

## Context
- Producer and consumers share the same event stream.
- Contract drift is a major failure source in event-driven systems.

## Decision
- Use one envelope shape: `eventId`, `eventType`, `payload`.
- Keep allowed event types in `CatalogEventSchema`.
- Validate contract before consumer business logic.
- Contract rule: payload changes are additive-only; breaking changes use a new event type.

## Alternatives Rejected
- **Scattered constants per service**
  - Rejected: high drift risk.
- **Schema Registry + Avro now**
  - Rejected at the current scope; good candidate for production evolution.

## Consequences
- Contract behavior is clear and testable.
- Payload fields are not centrally enforced in schema class in this scope.
- Still runtime JSON validation, not compile-time schema enforcement.

## Success Criteria
- Unknown/invalid events fail fast before side effects.
