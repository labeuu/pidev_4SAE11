# AImodel service (Spring AI + Ollama)

- **Code**: [backEnd/Microservices/AImodel](../../backEnd/Microservices/AImodel)
- **Eureka app name**: `AIMODEL`
- **Port**: **8095**
- **Gateway**: `http://localhost:8078/aimodel/**` → `http://localhost:8095` (direct URL in gateway config)

## Responsibilities

HTTP API that proxies **LLM** requests to **Ollama** (default model `qwen3:8b` in config). Used by other services (e.g. **Task** AI endpoints) and optionally by Offer UI flows.

Main files:

- [AImodelApplication.java](../../backEnd/Microservices/AImodel/src/main/java/com/esprit/aimodel/AImodelApplication.java)
- [AiController.java](../../backEnd/Microservices/AImodel/src/main/java/com/esprit/aimodel/controller/AiController.java)
- [LlmGenerationService.java](../../backEnd/Microservices/AImodel/src/main/java/com/esprit/aimodel/service/LlmGenerationService.java)

## Configuration

Environment variables:

- `OLLAMA_BASE_URL` (default `http://localhost:11434`)
- `OLLAMA_MODEL` (e.g. `qwen3:8b`)
- optional `SERVER_PORT`

## Run

Requires **Java 17+** and **Ollama** running locally.

```bash
cd backEnd/Microservices/AImodel
mvn spring-boot:run
```

The service still registers with **Eureka** by default (`AIMODEL`), but the **API Gateway** uses a direct URL to port **8095** so status and AI calls work even if discovery is slow.

## Data

No application database; stateless calls to Ollama.

## See also

- [api-gateway.md](../api-gateway.md) (long timeouts for AI routes)
- [task.md](task.md)
- [services-and-ports.md](../services-and-ports.md)
