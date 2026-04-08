# API Gateway

Spring Cloud Gateway application in [backEnd/apiGateway](../backEnd/apiGateway). Listens on **8078**. All browser traffic from the Angular app should go here (see [frontend.md](frontend.md)).

Configuration: [application.yml](../backEnd/apiGateway/src/main/resources/application.yml).

## Global behaviour

- **CORS**: `http://localhost:4200` and `http://127.0.0.1:4200`; methods GET/POST/PUT/DELETE/OPTIONS/PATCH; allows `Authorization`, `Content-Type`, etc.; `allowCredentials: true`.
- **Body size**: `spring.codec.max-in-memory-size: 10MB` (e.g. Base64 signatures).
- **HTTP client**: long default `response-timeout` (up to 4h) for slow local LLM chains; some routes override connect/response timeouts per route.

## Route table

Paths are **prefixed** as shown. The gateway applies **StripPrefix=1**: the first segment (e.g. `user`) is removed before the request hits the downstream service. So a call to `http://localhost:8078/user/api/...` becomes `/api/...` on the user service.

| Route id | Path predicate | Downstream | Notes |
|----------|----------------|------------|--------|
| keycloak-auth | `/keycloak-auth/**` | `http://localhost:8079` | Auth microservice |
| user | `/user/**` | `http://localhost:8090` | Direct URL |
| portfolio | `/portfolio/**` | `http://localhost:8086` | Direct; extra read timeout metadata |
| planning | `/planning/**` | `http://localhost:8081` | Direct; not via Eureka in gateway |
| review | `/review/**` | `http://localhost:8085` | Direct |
| project | `/project/**` | `http://localhost:8084` | Direct |
| gamification | `/gamification/**` | `http://localhost:8088` | Direct |
| offer | `/offer/**` | `lb://OFFER` | Eureka |
| contract | `/contract/**` | `http://localhost:8083` | Direct |
| notification | `/notification/**` | `http://localhost:8098` | Direct |
| ticket | `/ticket/**` | `http://localhost:8094` | Direct |
| subcontracting | `/subcontracting/**` | `http://localhost:8099` | Direct |
| task-ai | `/task/api/tasks/ai/**` | `http://localhost:8091` | **order: -1** (matched before generic task); 4h response timeout |
| task | `/task/**` | `http://localhost:8091` | Direct |
| vendor | `/vendor/**` | `http://localhost:8093` | Direct; long AI-related timeouts in config |
| aimodel | `/aimodel/**` | `lb://AIMODEL` | Node AI service; Eureka; long timeouts |

Service discovery locator is **disabled** (`discovery.locator.enabled: false`); every service must have an explicit route.

Services currently **without** gateway routes in `application.yml`: `FreelanciaJob` (8092), `chat` (8096), `meeting` (8097).

## Swagger via gateway

For services that expose SpringDoc, try:

`http://localhost:8078/<gateway-prefix>/swagger-ui.html`

Example: `http://localhost:8078/notification/swagger-ui.html` (when enabled on that service).

## Adding a new route

Copy an existing block in `application.yml`: `id`, `uri` (`http://localhost:<port>` or `lb://<EUREKA_APP_NAME>`), `Path`, `StripPrefix=1`. Register the service on Eureka if you use `lb://`.
