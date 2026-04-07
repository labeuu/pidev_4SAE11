# Review service

- **Code**: [backEnd/Microservices/review](../../backEnd/Microservices/review)
- **Spring name**: `review`
- **Port**: `8085`
- **Gateway**: `http://localhost:8078/review/**` (direct to `localhost:8085`)

## Responsibilities

**Reviews and ratings**, plus **responses** on reviews:

- **ReviewController**
- **ReviewResponseController** — can trigger **Notification** for the other party

## Data

- **MySQL** `reviewdb`

## Run

```bash
cd backEnd/Microservices/review
mvn spring-boot:run
```

## API docs

If enabled: `http://localhost:8078/review/swagger-ui.html`

## See also

- [architecture.md](../architecture.md) (Review → Notification)
- [services-and-ports.md](../services-and-ports.md)
