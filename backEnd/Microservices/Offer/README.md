# Offer microservice

Freelancer offers, applications, dashboards (`8082` via Config Server). Gateway: **`/offer/**` → Eureka `lb://OFFER`.

Start **Eureka**, **Config Server**, then this service.

**Documentation:** [Documentation/services/offer.md](../../../Documentation/services/offer.md)

```bash
cd backEnd/Microservices/Offer
mvn spring-boot:run
```
