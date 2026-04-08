# API Gateway

**Spring Cloud Gateway** — single HTTP entry point for the Angular app.

- **Port**: `8078`
- **Config**: [src/main/resources/application.yml](src/main/resources/application.yml)

Routes forward to microservices by path prefix (e.g. `/user/**`, `/project/**`). Some targets use **Eureka** (`lb://SERVICE_ID`); others use fixed `localhost` URLs.

## Documentation

- **Route table & CORS**: [Documentation/api-gateway.md](../../Documentation/api-gateway.md)
- **Architecture**: [Documentation/architecture.md](../../Documentation/architecture.md)

## Run

```bash
cd backEnd/apiGateway
mvn spring-boot:run
```

Start services you route to before testing end-to-end calls.
