# Planning microservice

Calendar, progress updates, GitHub integration (`8081`). Gateway: **`/planning/**`.

**Documentation:** [Documentation/services/planning.md](../../../Documentation/services/planning.md)

Start **Eureka** and **Config Server** before this service (configuration is loaded from Config Server, same pattern as Offer).

```bash
cd backEnd/Microservices/planning
mvn spring-boot:run
```
