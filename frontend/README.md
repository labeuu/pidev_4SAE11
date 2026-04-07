# Frontend

This directory contains the **Angular** client for the Smart Freelance platform.

## Application location

The runnable SPA is in **`smart-freelance-app/`** (Angular 21). From the repo root:

```bash
cd frontend/smart-freelance-app
npm install
npm start
```

Then open `http://localhost:4200`. The app talks to the **API Gateway** at `http://localhost:8078` by default (see `src/environments/environment.ts`).

## Documentation

- **Full guide**: [Documentation/frontend.md](../Documentation/frontend.md)
- **Gateway paths** (how URLs map to microservices): [Documentation/api-gateway.md](../Documentation/api-gateway.md)
- **Project hub**: [Documentation/README.md](../Documentation/README.md)

## Prerequisites

- Node.js 18+ and npm
- Backend: API Gateway and auth stack running for login and data (see root [README.md](../README.md))
