# Project service

- **Code**: [backEnd/Microservices/Project](../../backEnd/Microservices/Project)
- **Spring name**: `Project`
- **Port**: `8084`
- **Gateway**: `http://localhost:8078/project/**`

## Responsibilities

Client **projects** and **job postings**, and **applications** from freelancers:

- **ProjectController** — project lifecycle
- **ProjectApplicationController** — applications to projects

## Data

- **MySQL** `projectdb` (see `application.properties`)

## Run

```bash
cd backEnd/Microservices/Project
mvn spring-boot:run
```

## API docs

If enabled: `http://localhost:8078/project/swagger-ui.html`

## See also

- [services-and-ports.md](../services-and-ports.md)
