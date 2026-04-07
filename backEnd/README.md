# Backend

Java **Spring Boot** microservices, **Spring Cloud** infrastructure (Eureka, Config Server, API Gateway), **Keycloak** integration service, and one **Node.js** AI service.

## Layout

| Path | Role |
|------|------|
| `Eureka/` | Service discovery (port **8420**) |
| `ConfigServer/` | Centralised config (**8888**) |
| `apiGateway/` | HTTP gateway for the SPA (**8078**) |
| `KeyCloak/` | Auth microservice + Keycloak setup docs (**8079**) |
| `Microservices/` | Domain services (user, project, offer, …) |
| `scripts/` | Seed data and helper scripts |

## Quick start

1. MySQL (match ports in each service’s config — often `3306` or `3307`).
2. Start **Eureka**, then **Config Server** (needed for **Offer** and **Vendor**), then **API Gateway**.
3. Start **Keycloak** (standalone) and the **KeyCloak** Spring app — see [KeyCloak/README.md](KeyCloak/README.md).
4. Start the microservices you need; use Eureka dashboard to verify registered instances.

Example:

```bash
cd backEnd/Eureka
mvn spring-boot:run
```

## Documentation

- **Infrastructure** (Eureka, Config, Gateway, Keycloak): [Documentation/backend-infrastructure.md](../Documentation/backend-infrastructure.md)
- **Ports & databases**: [Documentation/services-and-ports.md](../Documentation/services-and-ports.md)
- **Gateway routes**: [Documentation/api-gateway.md](../Documentation/api-gateway.md)
- **Each microservice**: [Documentation/services/README.md](../Documentation/services/README.md)
- **Documentation hub**: [Documentation/README.md](../Documentation/README.md)

## Root README

High-level overview and feature list: [README.md](../README.md)
