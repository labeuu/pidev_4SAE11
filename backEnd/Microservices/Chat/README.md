# Chat Microservice

Real-time messaging service for the Smart Freelance Platform. Provides WebSocket-based instant messaging (STOMP over SockJS) and a REST API for message history, unread counts, and on-the-fly translation.

## Overview

| Property | Value |
|---|---|
| Spring application name | `chat` |
| Port | **8096** |
| Database | `chatdb` (MySQL 8) |
| Eureka registration | Yes |
| Config Server | Disabled (self-contained config) |

## Features

- **Real-time messaging** — STOMP over SockJS; messages delivered instantly to sender and receiver via user-destination queues
- **Typing indicators** — broadcast live typing events to the conversation partner
- **Read receipts** — mark messages as `SEEN`; sender is notified via WebSocket
- **Message history** — paginated REST endpoint for loading past conversations
- **Unread messages** — fetch all unread messages or a per-sender count map for a user
- **Conversation list** — returns the latest message per conversation thread with online status and unread badge count
- **Message translation** — powered by the MyMemory free API; auto-splits long text into ≤480-char chunks

## Tech Stack

- Java 17, Spring Boot 4.0.2
- Spring WebSocket (STOMP broker)
- Spring Data JPA + MySQL
- Spring Security (JWT auth interceptor for WebSocket handshake)
- Spring Cloud Netflix Eureka Client
- Lombok, Jackson

## Project Structure

```
src/main/java/tn/esprit/chat/
├── ChatApplication.java
├── config/
│   ├── SecurityConfig.java            # Permit-all REST, JWT auth for WS
│   ├── WebSocketAuthInterceptor.java  # Extracts user principal from JWT token
│   └── WebSocketConfig.java           # STOMP broker + SockJS endpoint config
├── controller/
│   ├── ChatController.java            # @MessageMapping WebSocket handlers
│   └── MessageRestController.java     # REST endpoints for history + translation
├── dto/
│   ├── ChatMessageDTO.java
│   ├── ConversationSummary.java
│   ├── SendMessageRequest.java
│   ├── TranslationRequest.java
│   ├── TranslationResponse.java
│   ├── TypingEvent.java
│   └── UserStatusEvent.java
├── entity/
│   ├── ChatMessage.java               # JPA entity (chat_messages table)
│   └── enums/MessageStatus.java       # SENT | DELIVERED | SEEN
├── repository/
│   └── ChatMessageRepository.java
└── service/
    ├── IChatService.java
    ├── ChatService.java
    ├── UserStatusService.java         # In-memory online/offline tracking
    └── TranslationService.java        # MyMemory API integration
```

## WebSocket API

**Endpoint:** `ws://localhost:8096/ws` (SockJS fallback included)

Connect with a JWT token — the `WebSocketAuthInterceptor` reads the `Authorization: Bearer <token>` header and sets the STOMP principal to the user's numeric ID.

### Outbound destinations (client → server)

| Destination | Payload | Description |
|---|---|---|
| `/app/chat` | `SendMessageRequest` | Send a message to another user |
| `/app/typing` | `TypingEvent` | Notify partner that you are typing |
| `/app/seen` | `{ "messageId": <Long> }` | Mark a message as read |

### Inbound destinations (server → client)

| Destination | Payload | Description |
|---|---|---|
| `/user/{id}/queue/messages` | `ChatMessageDTO` | New incoming message |
| `/user/{id}/queue/typing` | `TypingEvent` | Partner is typing |
| `/user/{id}/queue/seen` | `ChatMessageDTO` | Your message was seen |

### Example `SendMessageRequest`

```json
{
  "receiverId": 5,
  "content": "Hello!"
}
```

> `senderId` is set server-side from the authenticated principal — do not send it from the client.

## REST API

Base path: `/api/messages`

| Method | Path | Description |
|---|---|---|
| `GET` | `/conversation/{user1}/{user2}?page=0&size=20` | Paginated message history between two users |
| `GET` | `/unread/{userId}` | All unread messages for a user |
| `GET` | `/unread/count/{userId}` | `{ "total": N, "sender_<id>": N, ... }` |
| `PUT` | `/seen/{messageId}` | Mark a single message as SEEN |
| `GET` | `/conversations/{userId}` | Conversation list with last message + unread count |
| `POST` | `/translate` | Translate a text snippet |

### Translation request

```json
{
  "text": "Hello world",
  "targetLang": "fr",
  "sourceLang": "en"
}
```

`sourceLang` defaults to `"en"` when omitted or `"auto"`. The service uses the free MyMemory API and splits text longer than 480 characters into chunks automatically.

## Data Model

### `ChatMessage`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | Auto-increment |
| `sender_id` | BIGINT | References user service ID |
| `receiver_id` | BIGINT | References user service ID |
| `content` | TEXT | Message body |
| `timestamp` | DATETIME | Set at send time |
| `status` | VARCHAR | `SENT` → `DELIVERED` → `SEEN` |

## Configuration

All properties live in [src/main/resources/application.properties](src/main/resources/application.properties).

| Property | Default |
|---|---|
| `server.port` | `8096` |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/chatdb` |
| `eureka.client.service-url.defaultZone` | `http://localhost:8420/eureka/` |

No external credentials are required beyond a running MySQL instance. The database is created automatically on first start (`createDatabaseIfNotExist=true`).

## Running

```bash
cd backEnd/Microservices/Chat
mvn spring-boot:run
```

Prerequisites: MySQL running on port 3306, Eureka on port 8420.

## Running Tests

```bash
mvn test
```

Unit tests are in `ChatServiceTest` and use Mockito. Tests are organized into nested classes per method:

- `SendMessageTests` — persists with `SENT` status, sets timestamp
- `GetConversationTests` — paginated results, empty page
- `GetUnreadMessagesTests` — filters by status != SEEN
- `GetUnreadCountTests` — total + per-sender breakdown
- `MarkAsSeenTests` — updates status, throws on unknown ID
- `GetConversationsTests` — one summary per partner, online status, partner direction

## Integration with Other Services

The Chat service is standalone — it does not call other microservices via Feign. User IDs are numeric and come from the JWT token issued by Keycloak. Online status is tracked in-memory via `UserStatusService` and updated through WebSocket connect/disconnect events.

Access via API Gateway: `http://localhost:8078/chat/api/messages/...`
