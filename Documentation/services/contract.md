# Contract service

- **Code**: [backEnd/Microservices/Contract](../../backEnd/Microservices/Contract)
- **Spring name**: `Contract`
- **Port**: `8083`
- **Gateway**: `http://localhost:8078/contract/**`

## Responsibilities

**Contracts** between clients and freelancers, comments, and conflict handling:

- **ContractController**
- **CommentController**
- **ConflictController**

## Data

- **MySQL** `gestion_contract_db`

## Run

```bash
cd backEnd/Microservices/Contract
mvn spring-boot:run
```

## API docs

If enabled: `http://localhost:8078/contract/swagger-ui.html`

## See also

- [services-and-ports.md](../services-and-ports.md)
