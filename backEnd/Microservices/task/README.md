# Task microservice

Tasks, subtasks, comments, reports (`8091`). Gateway: **`/task/**`. AI endpoints under `/task/api/tasks/ai/**` use extended gateway timeouts.

**Documentation:** [Documentation/services/task.md](../../../Documentation/services/task.md)

Start **Eureka** and **Config Server** before this service (configuration is loaded from Config Server, same pattern as Offer).

```bash
cd backEnd/Microservices/task
mvn spring-boot:run
```
