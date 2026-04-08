# Ticket service (support)

- **Code**: [backEnd/Microservices/ticket-service](../../backEnd/Microservices/ticket-service)
- **Spring name**: `ticket`
- **Port**: `8094`
- **Gateway**: `http://localhost:8078/ticket/**`

## Responsibilities

Support **tickets** and **replies** (user and admin UIs in the Angular app):

- **TicketController**
- **ReplyController**

## Data

- **MySQL** `ticketdb`

## Run

```bash
cd backEnd/Microservices/ticket-service
mvn spring-boot:run
```

## API docs

If enabled: `http://localhost:8078/ticket/swagger-ui.html`

## See also

- [services-and-ports.md](../services-and-ports.md)
