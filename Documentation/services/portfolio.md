# Portfolio service

- **Code**: [backEnd/Microservices/Portfolio](../../backEnd/Microservices/Portfolio)
- **Spring name**: `PORTFOLIO` (Eureka)
- **Port**: `8086`
- **Gateway**: `http://localhost:8078/portfolio/**` → `http://localhost:8086`

## Responsibilities

Freelancer **portfolio**: profile views, **skills**, **experiences**, **evaluations** / tests, and related APIs:

- **ProfileViewController**, **SkillController**, **ExperienceController**
- **EvaluationController**, **EvaluationTestController**

## Data

- **MySQL** `portfolio_db`

## Run

Run the service on `8086`; the gateway forwards directly to this port.

```bash
cd backEnd/Microservices/Portfolio
mvn spring-boot:run
```

## API docs

If enabled: `http://localhost:8078/portfolio/swagger-ui.html`

## Further reading

- In-repo guide: [backEnd/Microservices/Portfolio/README.md](../../backEnd/Microservices/Portfolio/README.md)
- QA: [../TEST_PLAN_PORTFOLIO.md](../TEST_PLAN_PORTFOLIO.md)

## See also

- [services-and-ports.md](../services-and-ports.md)
