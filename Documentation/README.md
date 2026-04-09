# Documentation

Central index for the **Smart Freelance & Project Matching Platform**. Use these pages alongside the [root README](../README.md) for setup and architecture.

## Platform overview

| Topic | Document |
|--------|----------|
| Architecture & startup order | [architecture.md](architecture.md) |
| API Gateway routes & CORS | [api-gateway.md](api-gateway.md) |
| All services: ports, DBs, paths | [services-and-ports.md](services-and-ports.md) |
| Eureka, Config Server, Gateway, Keycloak | [backend-infrastructure.md](backend-infrastructure.md) |
| Angular app: routes, env, stack | [frontend.md](frontend.md) |

## Microservices (detail pages)

| Index | [services/README.md](services/README.md) |

Per-service guides live in [Documentation/services/](services/).

## Integrations & credentials

| Topic | Location |
|--------|----------|
| GitHub, Calendar, Firebase (no secrets in repo) | [credentials/README.md](../credentials/README.md) |
| Firebase files layout | [firebase-credentials/README.md](../firebase-credentials/README.md) |
| Backend seed / scripts | [backEnd/scripts/README-seed-data.md](../backEnd/scripts/README-seed-data.md) |
| Repo automation scripts | [scripts/README.md](../scripts/README.md) |

## Specs & QA

| Document | Purpose |
|----------|---------|
| [TEST_PLAN_PORTFOLIO.md](TEST_PLAN_PORTFOLIO.md) | Portfolio test plan |
| [UI_UX_Redesign_Spec.md](../UI_UX_Redesign_Spec.md) | UX redesign notes |
| [SCHEDULED_JOBS.md](../SCHEDULED_JOBS.md) | Scheduled jobs |
| [plans/](../plans/) | Implementation specs |

## Repository layout (quick map)

| Path | Role |
|------|------|
| [frontend/](../frontend/) | Angular SPA (`smart-freelance-app`) |
| [backEnd/](../backEnd/) | Eureka, Config Server, API Gateway, Keycloak service, `Microservices/`, `scripts/` |
| [Documentation/](.) | This documentation set |
| [scripts/](../scripts/) | SQL seed + GitHub token helper ([scripts/README.md](../scripts/README.md)) |
| [plans/](../plans/) | Implementation specs (e.g. UX implementation notes) |
| [credentials/](../credentials/) | Local credential layout (no secrets committed) |
| [firebase-credentials/](../firebase-credentials/) | Firebase key layout (gitignored keys) |
| [logs/](../logs/) | Optional: log and PID files when using root `start-backend.*` / `stop-backend.*` |
| Root | [README.md](../README.md), [start-backend.ps1](../start-backend.ps1) / [.sh](../start-backend.sh), [stop-backend.ps1](../stop-backend.ps1) / [.sh](../stop-backend.sh) |

**Doc naming note:** Service detail pages use mixed filename styles (e.g. `ticket-service.md`, `AImodel.md`) to match historical paths; the [services index](services/README.md) is the canonical table of links.

Folder-level **README** files in `frontend/`, `backEnd/`, and each major backend module link back here for full detail.
