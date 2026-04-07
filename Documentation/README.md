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
| [backEnd/](../backEnd/) | Eureka, Config Server, API Gateway, Keycloak service, `Microservices/` |
| [Documentation/](.) | This documentation set |

Folder-level **README** files in `frontend/`, `backEnd/`, and each major backend module link back here for full detail.
