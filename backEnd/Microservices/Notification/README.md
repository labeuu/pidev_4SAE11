# Notification Microservice

**Full documentation:** [Documentation/services/notification.md](../../Documentation/services/notification.md)

Spring Boot microservice for user notifications, backed by **Firebase Firestore**.

- **Port:** 8087
- **Eureka:** Registers as `notification`
- **Gateway:** Exposed at `http://localhost:8078/notification/**` (StripPrefix=1)

## Prerequisites

1. **Firebase service account key** (JSON) from Firebase Console → Project settings → Service accounts → Generate new private key.
2. Set one of:
   - **Environment variable:** `GOOGLE_APPLICATION_CREDENTIALS=<absolute-path-to-json>`
   - **Config property:** `notification.firebase.credentials-path=<path-to-json>` (in `notification.properties` or local `application.properties`)

Do **not** commit the JSON file; it is ignored via `.gitignore`.

## Running

1. Start **Config Server** (8888), **Eureka** (8420), and optionally **API Gateway** (8078).
2. Set `GOOGLE_APPLICATION_CREDENTIALS` or `notification.firebase.credentials-path` to your Firebase key path.
3. Run the application (e.g. from IDE or `mvn spring-boot:run` from this directory).

## How other microservices can use this service

- **Via API Gateway:**  
  `POST http://localhost:8078/notification/api/notifications`  
  Body: `{ "userId": "...", "title": "...", "body": "...", "type": "PROJECT_UPDATE", "data": {} }`

- **Direct (same host):**  
  `POST http://localhost:8087/api/notifications`

- **Via Feign (from another microservice):**  
  Define a `@FeignClient(name = "notification", path = "/api/notifications")` interface and call `create(NotificationRequest)`.

### Main endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST   | /api/notifications           | Create notification (for a user) |
| GET    | /api/notifications/user/{userId} | List notifications for user |
| PATCH  | /api/notifications/{id}/read | Mark as read |
| DELETE | /api/notifications/{id}     | Delete notification |
| GET    | /welcome                     | Service info |
| GET    | /swagger-ui.html             | Swagger UI |

## Firestore

Data is stored in the `notifications` collection. Documents contain: `userId`, `title`, `body`, `type`, `read`, `createdAt`, and optional `data` map.
