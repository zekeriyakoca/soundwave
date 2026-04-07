# Security

## Scope Note
Authentication and authorization are intentionally out of scope for the current phase. The API is open. This document only covers the operational exposure surface this project actually creates and the immediate steps to harden it.

## Currently Exposed Endpoints

Actuator endpoints exposed via `management.endpoints.web.exposure.include`:

| Endpoint | Purpose | Sensitivity |
|---|---|---|
| `/actuator/health` | Liveness/readiness probe | Low |
| `/actuator/info` | Build metadata | Low |
| `/actuator/metrics` | Micrometer metrics index | Medium |
| `/actuator/prometheus` | Prometheus scrape endpoint | Medium |

App-specific metrics (non-exhaustive):
- `outbox_events_pending`, `outbox_events_failed`, `outbox_dlt_events_total`
- `http_server_requests_seconds_*`

These leak internal topology (route names, consumer groups, queue depth) and should not be exposed to the public internet.

## Immediate Hardening Plan
- Restrict the management port to Prometheus only.
- Restrict full `/actuator/health` details to internal probes; keep liveness/readiness minimal.
- Add an auth layer (Spring Security + JWT/OIDC) in front of `/api/**` before any non-trusted exposure.
