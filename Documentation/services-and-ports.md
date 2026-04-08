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
| Microservices/planning | planning | 8081 | `planning` | MySQL `planningdb` (Config Server) |
| Microservices/Offer | OFFER | 8082 | `offer` | MySQL `gestion_offre_db` (Config Server) |
| Microservices/Contract | Contract | 8083 | `contract` | MySQL `gestion_contract_db` |
| Microservices/Project | Project | 8084 | `project` | MySQL `projectdb` |
| Microservices/review | review | 8085 | `review` | MySQL `reviewdb` |
| Microservices/Portfolio | PORTFOLIO | 8086 | `portfolio` | MySQL `portfolio_db` |
| Microservices/Notification | notification | 8098 | `notification` | Firebase / Firestore (credentials via env) |
| Microservices/gamification | gamification | 8088 | `gamification` | MySQL `gamificationdb` |
| Microservices/task | task | 8091 | `task` | MySQL `taskdb` (Config Server) |
| Microservices/FreelanciaJob | FreelanciaJob | 8092 | — | MySQL `freelancia_job_db` |
| Microservices/AImodel | AIMODEL (Eureka) | 8095 (default `PORT`) | `aimodel` | Ollama HTTP API (no app DB) |
| Microservices/Chat | chat | 8096 | — | MySQL `chatdb` |
| Microservices/Meeting | meeting | 8097 | — | MySQL `meetingdb` |
| Microservices/Vendor | VENDOR | 8093 | `vendor` | MySQL `gestion_vendor_db` (Config Server) |
| Microservices/ticket-service | ticket | 8094 | `ticket` | MySQL `ticketdb` |
| Microservices/Subcontracting | SUBCONTRACTING | 8099 | `subcontracting` | MySQL `gestion_subcontracting_db` (Config Server) |

**Gateway routing notes**

- **Eureka (`lb://`)**: OFFER, AIMODEL — start **Eureka** and register these instances before relying on gateway resolution.
- **Direct URLs**: user, portfolio, planning, review, project, gamification, contract, notification, task, vendor, ticket, subcontracting — gateway sends to fixed `localhost` ports.
- **No gateway route (current)**: FreelanciaJob, Chat, Meeting (service-to-service and/or direct local access only unless routes are added).

**Special task route**

- `/task/api/tasks/ai/**` is ordered before `/task/**` so AI endpoints get extended timeouts on the gateway.

Detail pages: [Documentation/services/](services/README.md)
