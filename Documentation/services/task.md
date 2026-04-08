# Task service

- **Code**: [backEnd/Microservices/task](../../backEnd/Microservices/task)
- **Spring name**: `task`
- **Port**: `8091` (from [Config Server task.properties](../../backEnd/ConfigServer/src/main/resources/config/task.properties))
- **Gateway**: `http://localhost:8078/task/**`

## Configuration

Bootstrap matches **Offer**: only `spring.application.name` and `spring.config.import=configserver:http://localhost:8888` (+ optional `application-local.properties`) in `application.properties`. Start **Eureka (8420)** and **Config Server (8888)** before this service.

## Responsibilities

**Tasks**, **subtasks**, comments, reports, **AI-assisted** task endpoints, and statistics:

- **TaskController**, **SubtaskController**, **TaskCommentController**
- **TaskAiController** — AI flows (gateway matches `/task/api/tasks/ai/**` first with long timeouts)
- **TaskReportController**, **TaskStatsController**, **TaskHealthController**

## Data

- **MySQL** `taskdb`

## Integrations

May call the **AImodel** Node service (Ollama) for AI operations — ensure Ollama and AImodel are up for those endpoints.

## Run

```bash
cd backEnd/Microservices/task
mvn spring-boot:run
```

## API docs

If enabled: `http://localhost:8078/task/swagger-ui.html`

## See also

- [api-gateway.md](../api-gateway.md) (task-ai route ordering)
- [AImodel.md](AImodel.md)
- [services-and-ports.md](../services-and-ports.md)
