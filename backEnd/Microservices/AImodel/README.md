# AImodel service (Spring AI + Ollama)

Ollama-backed LLM API on port **8095**. Registers in Eureka as **`AIMODEL`**. API Gateway: **`/aimodel/**`**.

Requires **Java 17+**, **Ollama** at `http://localhost:11434` (default), and the configured model pulled (e.g. `ollama pull qwen3:8b`).

**Documentation:** [Documentation/services/AImodel.md](../../../Documentation/services/AImodel.md)

```bash
cd backEnd/Microservices/AImodel
mvn spring-boot:run
```

(Ensure **Config Server** and **Eureka** are running if you use the default bootstrap imports.)
