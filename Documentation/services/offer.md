# Offer service

- **Code**: [backEnd/Microservices/Offer](../../backEnd/Microservices/Offer)
- **Spring name**: `OFFER` (Eureka)
- **Port**: `8082` (from [Config Server OFFER.properties](../../backEnd/ConfigServer/src/main/resources/config/OFFER.properties))
- **Gateway**: `http://localhost:8078/offer/**` → `lb://OFFER`

## Responsibilities

Freelancer **offers**, **applications**, dashboards, and related features:

- **OfferController**, **OfferApplicationController**
- **DashboardController**
- **ChatAssistantController** (AI-assisted chat for offers — optional integrations)

## Configuration

Bootstrap loads **Config Server** (`configserver:http://localhost:8888`). Start **Eureka (8420)** and **Config Server (8888)** before this service.

Local overrides: `application-local.properties` (gitignored pattern).

## Data

- **MySQL** `gestion_offre_db`

## Run

```bash
cd backEnd/Microservices/Offer
mvn spring-boot:run
```

## API docs

If enabled: `http://localhost:8078/offer/swagger-ui.html`

## See also

- [backend-infrastructure.md](../backend-infrastructure.md)
- [services-and-ports.md](../services-and-ports.md)
