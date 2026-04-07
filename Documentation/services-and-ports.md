# Services, ports, and data stores

Reference for local development. **Gateway prefix** is the first URL segment the Angular app uses (`http://localhost:8078/<prefix>/...`).

MySQL hosts vary in repo configs: many services use `localhost:3306`, some **3307** — align your MySQL install with each service’s `application.properties` or Config Server file.

| Folder (under `backEnd/`) | Application / Eureka name | Port | Gateway prefix | Primary data store / integration |
|---------------------------|---------------------------|------|----------------|-----------------------------------|
| Eureka | Eureka | 8420 | — | N/A |
| ConfigServer | config-server | 8888 | — | N/A |
| apiGateway | apiGateway | 8078 | — | N/A |
| KeyCloak | keycloak-auth | 8079 | `keycloak-auth` | Keycloak (external) |
| Microservices/user | user | 8090 | `user` | MySQL `userdb` (example URL uses 3307) |
| Microservices/planning | planning | 8081 | `planning` | MySQL `planningdb` |
| Microservices/Offer | OFFER | 8082 | `offer` | MySQL `gestion_offre_db` (Config Server) |
| Microservices/Contract | Contract | 8083 | `contract` | MySQL `gestion_contract_db` |
| Microservices/Project | Project | 8084 | `project` | MySQL `projectdb` |
| Microservices/review | review | 8085 | `review` | MySQL `reviewdb` |
| Microservices/Portfolio | PORTFOLIO | 8086 | `portfolio` | MySQL `portfolio_db` |
| Microservices/Notification | notification | 8087 | `notification` | Firebase / Firestore (credentials via env) |
| Microservices/gamification | gamification | 8088 | `gamification` | MySQL `gamificationdb` |
| Microservices/task | task | 8091 | `task` | MySQL `taskdb` |
| Microservices/AImodel | AIMODEL (Eureka) | 8092 (default `PORT`) | `aimodel` | Ollama HTTP API (no app DB) |
| Microservices/Vendor | VENDOR | 8093 | `vendor` | MySQL `gestion_vendor_db` (Config Server) |
| Microservices/ticket-service | ticket | 8094 | `ticket` | MySQL `ticketdb` |

**Gateway routing notes**

- **Eureka (`lb://`)**: PORTFOLIO, OFFER, AIMODEL — start **Eureka** and register these instances before relying on gateway resolution.
- **Direct URLs**: user, planning, review, project, gamification, contract, notification, task, vendor, ticket — gateway sends to fixed `localhost` ports.

**Special task route**

- `/task/api/tasks/ai/**` is ordered before `/task/**` so AI endpoints get extended timeouts on the gateway.

Detail pages: [Documentation/services/](services/README.md)
