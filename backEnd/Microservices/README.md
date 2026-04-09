# Microservices

Each subdirectory is an independent service (own database and/or integrations unless noted). Traffic from the web app goes through the **API Gateway** (`8078`), not directly to these ports.

## Index

| Folder | Gateway prefix | Doc |
|--------|----------------|-----|
| `user/` | `/user` | [Documentation/services/user.md](../../Documentation/services/user.md) |
| `Project/` | `/project` | [Documentation/services/project.md](../../Documentation/services/project.md) |
| `Offer/` | `/offer` | [Documentation/services/offer.md](../../Documentation/services/offer.md) |
| `Contract/` | `/contract` | [Documentation/services/contract.md](../../Documentation/services/contract.md) |
| `Portfolio/` | `/portfolio` | [Documentation/services/portfolio.md](../../Documentation/services/portfolio.md) |
| `review/` | `/review` | [Documentation/services/review.md](../../Documentation/services/review.md) |
| `planning/` | `/planning` | [Documentation/services/planning.md](../../Documentation/services/planning.md) |
| `Notification/` | `/notification` | [Documentation/services/notification.md](../../Documentation/services/notification.md) |
| `task/` | `/task` | [Documentation/services/task.md](../../Documentation/services/task.md) |
| `gamification/` | `/gamification` | [Documentation/services/gamification.md](../../Documentation/services/gamification.md) |
| `Vendor/` | `/vendor` | [Documentation/services/vendor.md](../../Documentation/services/vendor.md) |
| `ticket-service/` | `/ticket` | [Documentation/services/ticket-service.md](../../Documentation/services/ticket-service.md) |
| `Subcontracting/` | `/subcontracting` | [Documentation/services/subcontracting.md](../../Documentation/services/subcontracting.md) |
| `AImodel/` (Spring Boot + Ollama) | `/aimodel` | [Documentation/services/AImodel.md](../../Documentation/services/AImodel.md) |
| `FreelanciaJob/` | _(none)_ | [Documentation/services/freelanciajob.md](../../Documentation/services/freelanciajob.md) |
| `Chat/` | _(none)_ | [Documentation/services/chat.md](../../Documentation/services/chat.md) |
| `Meeting/` | _(none)_ | [Documentation/services/meeting.md](../../Documentation/services/meeting.md) |

## Reference tables

- All ports and MySQL DB names: [Documentation/services-and-ports.md](../../Documentation/services-and-ports.md)
- Gateway path rules: [Documentation/api-gateway.md](../../Documentation/api-gateway.md)

## Run (typical)

```bash
cd backEnd/Microservices/<ServiceFolder>
mvn spring-boot:run
```

**AImodel** uses Spring Boot: run with Maven (`mvn spring-boot:run`).

**Offer**, **Vendor**, **Task**, **Planning**, **Subcontracting**, and **AIMODEL** require **Config Server**. **OFFER** uses **Eureka** for the gateway `lb://OFFER` route — start Eureka first. **AIMODEL** registers with Eureka but the gateway calls it via a direct URL (`localhost:8095`).
