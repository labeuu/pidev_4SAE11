# Notification service

- **Code**: [backEnd/Microservices/Notification](../../backEnd/Microservices/Notification)
- **Spring name**: `notification`
- **Port**: `8098`
- **Gateway**: `http://localhost:8078/notification/**` (direct)

## Responsibilities

Push / in-app **notifications** backed by **Firebase** (Firestore); health endpoint for ops:

- **NotificationController**
- **NotificationHealthController**

## Configuration

- Firebase credentials path via `GOOGLE_APPLICATION_CREDENTIALS` or `notification.firebase.credentials-path` / local override — see service `application.properties` and [firebase-credentials/README.md](../../firebase-credentials/README.md).
- Optional Config Server import: `optional:configserver:http://localhost:8888`

## Run

```bash
cd backEnd/Microservices/Notification
mvn spring-boot:run
```

## API docs

Service configures SpringDoc paths explicitly; via gateway:

`http://localhost:8078/notification/swagger-ui.html`

## Further reading

- [backEnd/Microservices/Notification/README.md](../../backEnd/Microservices/Notification/README.md)

## See also

- [services-and-ports.md](../services-and-ports.md)
