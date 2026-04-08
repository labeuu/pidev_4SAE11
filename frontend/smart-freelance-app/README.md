# Smart Freelance App (Frontend)

Angular **21** SPA for the Smart Freelance and Project Matching Platform. All HTTP calls go through the **API Gateway** (see `src/environments/environment.ts`, default `http://localhost:8078`).

**Full documentation:** [Documentation/frontend.md](../../Documentation/frontend.md)

## Main features

- **Public** — Home, How it works, About
- **Auth** — Login, sign up, forgot password (Keycloak via gateway `keycloak-auth` prefix)
- **Dashboard** (roles **CLIENT** and **FREELANCER**) — Projects, job browse, applications, offers, portfolio, tasks, calendar, GitHub, progress, reviews, support tickets, gamification, notifications, contracts, vendor/agrément flows, freelancer search, profile
- **Admin** (`/admin`) — Users, contracts, offers, vendors, projects, planning, tasks, tickets, calendar, GitHub, skill stats, contract stats, skill management, reviews, achievements

Routes use `authGuard` and `roleGuard`; see `src/app/app.routes.ts`.

## Development server

```bash
npm install
npm start
```

Opens `http://localhost:4200/` with live reload.

## Build

```bash
npm run build
```

Artifacts go under `dist/`. Use `npm run build -- --configuration production` when you need the production environment file.

## Unit and e2e tests

This project’s `angular.json` does **not** define a `test` (or `e2e`) target under `architect`. Add one if you introduce automated tests; until then, `ng test` / `ng e2e` may not be available.

## Scaffolding

```bash
ng generate component component-name
ng generate --help
```

## Resources

- [Angular CLI](https://angular.dev/tools/cli)
