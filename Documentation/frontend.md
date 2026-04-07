# Frontend

The web UI is an **Angular 21** SPA in [frontend/smart-freelance-app](../frontend/smart-freelance-app).

## Stack

From [package.json](../frontend/smart-freelance-app/package.json):

- Angular 21, TypeScript ~5.9, RxJS 7.8
- Bootstrap 5, Chart.js / ng2-charts, jspdf, angularx-qrcode
- Package manager: **npm** (see `packageManager` field)

## API base URL

[environment.ts](../frontend/smart-freelance-app/src/environments/environment.ts) sets:

- `apiGatewayUrl: 'http://localhost:8078'` — all REST calls should target the **API Gateway**, not individual microservice ports.
- `authApiPrefix: 'keycloak-auth/api/auth'` — combined with the gateway URL for login/signup.

Do **not** commit real API keys or secrets; use local env overrides where needed.

## Run locally

```bash
cd frontend/smart-freelance-app
npm install
npm start
```

Opens `http://localhost:4200` (dev server). Production build: `npm run build` (outputs under `dist/`).

## Build configurations

[angular.json](../frontend/smart-freelance-app/angular.json) uses `environment.production.ts` / `environment.development.ts` via `fileReplacements`. There is **no** `test` target in `architect` currently — unit tests are not wired in this project’s CLI config.

## Application routes (summary)

Defined in [app.routes.ts](../frontend/smart-freelance-app/src/app/app.routes.ts).

| Area | Path prefix | Guard | Notes |
|------|-------------|--------|--------|
| Public | `''`, `how-it-works`, `about` | None | Public layout |
| Auth | `login`, `signup`, `forgot-password` | None | |
| Dashboard | `dashboard/**` | `authGuard`, `roleGuard` (CLIENT, FREELANCER) | Projects, offers, portfolio, tasks, calendar, GitHub, reviews, tickets, gamification, contracts, vendors, etc. |
| Admin | `admin/**` | `authGuard`, `roleGuard` (ADMIN) | Users, contracts, offers, vendors, projects, planning, tasks, tickets, reviews, achievements, skills, stats |

Sensitive routes redirect unauthenticated users (see `authGuard` / `roleGuard` in `src/app/core/guards/`).

## Feature-oriented route examples (dashboard)

- **Projects**: `dashboard/my-projects`, `dashboard/browse-jobs`, `dashboard/my-applications`
- **Offers**: `dashboard/my-offers`, `dashboard/browse-offers`
- **Portfolio / profile**: `dashboard/my-portfolio`, `dashboard/profile`, `dashboard/freelancer-portfolio/:id`
- **Planning / GitHub**: `dashboard/calendar`, `dashboard/github`, `dashboard/track-progress`, `dashboard/progress-updates`
- **Tasks**: `dashboard/my-tasks`, `dashboard/project-tasks`
- **Reviews**: `dashboard/reviews/**`
- **Tickets**: `dashboard/tickets/**`
- **Gamification**: `dashboard/gamification`
- **Vendors**: `dashboard/my-vendors`, `dashboard/client-vendors`
- **Contracts**: `dashboard/my-contracts`

## Related

- [api-gateway.md](api-gateway.md) — path prefixes the frontend should use through port 8078.
- [frontend/README.md](../frontend/README.md) — repository folder overview.
