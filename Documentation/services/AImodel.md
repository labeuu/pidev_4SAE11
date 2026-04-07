# AImodel service (Node.js)

- **Code**: [backEnd/Microservices/AImodel](../../backEnd/Microservices/AImodel)
- **Eureka app name**: `AIMODEL` (default; overridable via `EUREKA_APP_NAME`)
- **Port**: **8092** (default; overridable via `PORT`)
- **Gateway**: `http://localhost:8078/aimodel/**` → `lb://AIMODEL`

## Responsibilities

HTTP API that proxies **LLM** requests to **Ollama** (default model `qwen3:8b` in config). Used by other services (e.g. **Task** AI endpoints) and optionally by Offer UI flows.

Main files:

- [server.js](../../backEnd/Microservices/AImodel/src/server.js) — listens and registers with Eureka
- [routes/aiRoutes.js](../../backEnd/Microservices/AImodel/src/routes/aiRoutes.js), **controllers**, **services**, **ollamaClient**

## Configuration

Environment variables (see [config/index.js](../../backEnd/Microservices/AImodel/src/config/index.js)):

- `PORT` (default 8092)
- `OLLAMA_BASE_URL` (default `http://localhost:11434`)
- `OLLAMA_MODEL`, `OLLAMA_TIMEOUT_MS`, etc.
- `EUREKA_ENABLED`, `EUREKA_SERVER_URL`, instance hostname/IP

## Run

Requires **Node 18+** and (for full functionality) **Ollama** running locally.

```bash
cd backEnd/Microservices/AImodel
npm install
npm start
```

Start **Eureka** before the service if `EUREKA_ENABLED` is true (default), so the gateway can use `lb://AIMODEL`.

## Data

No application database; stateless calls to Ollama.

## See also

- [api-gateway.md](../api-gateway.md) (long timeouts for AI routes)
- [task.md](task.md)
- [services-and-ports.md](../services-and-ports.md)
