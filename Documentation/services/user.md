# User service

- **Code**: [backEnd/Microservices/user](../../backEnd/Microservices/user)
- **Spring name**: `user`
- **Port**: `8090`
- **Gateway**: `http://localhost:8078/user/**` (StripPrefix → service sees paths without `/user`)

## Responsibilities

User accounts and profile-related HTTP APIs, including:

- **UserController** — user CRUD and profile operations used by the platform
- **AvatarController** — avatar upload/handling

## Data

- **MySQL** (`userdb` in `application.properties`; sample config uses port **3307** — match your local MySQL)

## Run

```bash
cd backEnd/Microservices/user
mvn spring-boot:run
```

## API docs

If SpringDoc is enabled, try: `http://localhost:8078/user/swagger-ui.html`

## See also

- [services-and-ports.md](../services-and-ports.md)
