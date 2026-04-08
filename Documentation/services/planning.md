# Planning service

- **Code**: [backEnd/Microservices/planning](../../backEnd/Microservices/planning)
- **Spring name**: `planning`
- **Port**: `8081` (from [Config Server planning.properties](../../backEnd/ConfigServer/src/main/resources/config/planning.properties))
- **Gateway**: `http://localhost:8078/planning/**` (direct to `localhost:8081`)

## Configuration

Bootstrap matches **Offer**: only `spring.application.name` and `spring.config.import=configserver:http://localhost:8888` (+ optional `application-local.properties`) in `application.properties`. Start **Eureka (8420)** and **Config Server (8888)** before this service.

## Responsibilities

**Calendar**, **progress updates**, stats, comments, and **GitHub** integration for tracking work:

- **CalendarController**
- **ProgressUpdateController**, **ProgressUpdateStatsController**, **ProgressCommentController**
- **GitHubController**
- **PlanningHealthController**

## Data

- **MySQL** `planningdb`

## Run

```bash
cd backEnd/Microservices/planning
mvn spring-boot:run
```

## API docs

If enabled: `http://localhost:8078/planning/swagger-ui.html`

## Integrations

GitHub tokens and related setup: [credentials/README.md](../../credentials/README.md)

## See also

- [services-and-ports.md](../services-and-ports.md)
