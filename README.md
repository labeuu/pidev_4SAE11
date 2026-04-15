<div align="center">

[![ESPRIT Tunisia](https://upload.wikimedia.org/wikipedia/commons/b/b6/Logo_ESPRIT_-_Tunisie.png)](https://www.esprit.tn)

**École Supérieure Privée d'Ingénierie et de Technologie — Tunisie**

# Smart Freelance & Project Matching Platform

*PI Dev — 4SAE11 — Academic Year 2024/2025*

A microservices-based platform connecting freelancers and clients for project collaboration, featuring AI-powered skill verification, portfolio management, real-time notifications, and GitHub integration.

[Features](#features) • [Architecture](#architecture) • [Getting Started](#getting-started) • [Documentation](#documentation)

</div>

---

## Overview

**Smart Freelance & Project Matching Platform** is a full-stack web application built with a microservices architecture. Clients can post projects, browse freelancer profiles, and hire talent; freelancers can showcase portfolios, apply to jobs, manage offers, and track progress with calendar and GitHub integration. Role-specific dashboards (Client, Freelancer, Admin) provide tailored KPIs, quick actions, and feeds.

---

## Features

| Role | Capabilities |
|------|--------------|
| **Freelancers** | Portfolio with experiences & skills, AI skill verification, browse jobs, submit applications, manage offers, reviews & ratings (with response messaging), contracts, notifications, calendar, GitHub sync |
| **Clients** | Project CRUD, job posting, browse freelancers/offers, progress tracking, track progress updates |
| **Admins** | User management, projects/contracts/offers oversight, planning, reviews, skill management, GitHub, evaluations |

---

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌────────────────────────────────────────────────────────────┐
│   Angular   │──── │ API Gateway  │──── │ User │ Project │ Offer │ Contract │ Portfolio │ Review  │
│  Frontend   │     │   (8078)     │     │ Planning │ Notification │ Task │ Gamification │ Vendor │
│  (4200)     │     └──────────────┘     │ Ticket │ AImodel (Node) │ Keycloak auth MS              │
└─────────────┘            │             └────────────────────────────────────────────────────────────┘
                           │
                    ┌──────┴──────┐
                    │   Config    │
                    │   Server    │
                    │   (8888)    │
                    └─────────────┘
```

**Inter-service calls:** Review → Notification (when a review response is received); Planning/Task/Offer → Notification.

### Tech Stack

| Layer | Technologies |
|-------|--------------|
| **Frontend** | Angular 21, Bootstrap 5, Chart.js, TypeScript 5.9, SCSS, View Transitions (route animations) |
| **Backend** | Java 17, Spring Boot 3.4/4.0, Spring Cloud (Eureka, Config, Gateway), OpenFeign, Resilience4j |
| **Security** | Keycloak (OAuth2/JWT) |
| **Database** | MySQL 8 (one DB per microservice) |
| **APIs** | SpringDoc / OpenAPI (Swagger) |
| **Extras** | Firebase (notifications), AI API (skill verification), GitHub integration |

---

## Getting Started

### Prerequisites

- **Java 17**
- **Maven 3.8+**
- **Node.js 18+** and **npm**
- **MySQL 8** — repos use `localhost:3307` (see [Documentation/services-and-ports.md](Documentation/services-and-ports.md))
- **Keycloak** (standalone) on `localhost:8080` with realm `smart-freelance`

### Service Ports

| Service | Port | Database |
|---------|------|----------|
| Eureka | 8420 | — |
| Config Server | 8888 | — |
| API Gateway | 8078 | — |
| Keycloak Auth | 8079 | — |
| User | 8090 | `userdb` |
| Planning | 8081 | `planningdb` |
| Offer | 8082 | `gestion_offre_db` |
| Contract | 8083 | `gestion_contract_db` |
| Project | 8084 | `projectdb` |
| Review | 8085 | `reviewdb` |
| Portfolio | 8086 | `portfolio_db` |
| Notification | 8087 | Firebase |
| Gamification | 8088 | `gamificationdb` |
| Task | 8091 | `taskdb` |
| AImodel (Node) | 8092 | — (Ollama) |
| Vendor | 8093 | `gestion_vendor_db` |
| Ticket | 8094 | `ticketdb` |

### Startup Order

1. MySQL  
2. **Eureka** → `backEnd/Eureka`  
3. **Config Server** → `backEnd/ConfigServer` *(required for **Offer** and **Vendor**; optional for others)*  
4. **API Gateway** → `backEnd/apiGateway`  
5. **Keycloak** (standalone) — [see Keycloak setup](backEnd/KeyCloak/README.md)  
6. **Keycloak Auth** → `backEnd/KeyCloak`  
7. **Microservices** — User, Project, Offer, Contract, Portfolio, Review, Planning, Notification, Task, Gamification, Vendor, Ticket, **AImodel** (Node + Ollama if using AI)  

### Run the Backend

```bash
# Example: start Eureka
cd backEnd/Eureka
mvn spring-boot:run

# Example: start User service
cd backEnd/Microservices/user
mvn spring-boot:run
```

### Run the Frontend

```bash
cd frontend/smart-freelance-app
npm install
npm start
```

Open **http://localhost:4200**

### API Documentation

Swagger UI is available via the Gateway for services that expose it (use the gateway path prefix in the URL).

Example: `http://localhost:8078/user/swagger-ui.html`

Full route and port reference: [Documentation/api-gateway.md](Documentation/api-gateway.md).

---

## Project Structure

```
├── backEnd/
│   ├── apiGateway/          # Spring Cloud Gateway
│   ├── ConfigServer/        # Centralized configuration
│   ├── Eureka/              # Service discovery
│   ├── KeyCloak/            # Auth microservice (OAuth2/JWT)
│   └── Microservices/
│       ├── Contract/        # Contract management
│       ├── Notification/    # Push notifications (Firebase)
│       ├── Offer/           # Offers & applications
│       ├── planning/        # Calendar, GitHub sync, progress updates
│       ├── task/            # Tasks, subtasks, AI-assisted endpoints
│       ├── gamification/    # Achievements, levels, XP
│       ├── Vendor/          # Vendor / agrément workflows
│       ├── ticket-service/   # Support tickets
│       ├── AImodel/         # Node + Ollama LLM API
│       ├── Portfolio/       # Portfolio, skills, AI verification
│       ├── Project/         # Project management
│       ├── review/          # Reviews & ratings (sends notifications on response)
│       └── user/            # User profiles
├── frontend/
│   └── smart-freelance-app/ # Angular SPA
├── Documentation/           # Architecture, gateway, per-service docs (see README there)
└── plans/                   # Implementation specs
```

---

## Optional Integrations

| Integration | Purpose | Configuration |
|-------------|---------|---------------|
| **Google Translate** | Offer translations | API key in Offer service |
| **Firebase** | Push notifications (Firestore) | Credentials in Notification service |
| **GitHub** | Planning sync, commit history | See [credentials/README.md](credentials/README.md) — token in `githubToken.txt` or `$env:GITHUB_TOKEN` (never committed) |
| **AI API** | Skill verification | API key in Portfolio service |

All credential files are gitignored. See [credentials/README.md](credentials/README.md) for setup.

---

## Documentation

- **[Documentation hub](Documentation/README.md)** — architecture, gateway, frontend guide, per-service pages
- [Keycloak Setup](backEnd/KeyCloak/README.md) — Auth & realm configuration
- [Credentials Setup](credentials/README.md) — GitHub token, Google Calendar, Firebase
- [Portfolio Test Plan](Documentation/TEST_PLAN_PORTFOLIO.md)
- [Implementation Specs](plans/)

---

## Contributing

This project is developed as part of the **PI Dev** course at ESPRIT Tunisia. For contributions, please follow the existing code style and open a pull request.

---

<div align="center">

**ESPRIT — École Supérieure Privée d'Ingénierie et de Technologie — Tunisie**

*4SAE11 • PI Dev • 2024/2025*

`#Angular` `#SpringBoot` `#Microservices` `#Keycloak` `#MySQL` `#Freelance` `#FullStack` `#TypeScript` `#Java` `#ESPIRIT` `#Tunisia`

</div>
