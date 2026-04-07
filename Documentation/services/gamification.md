# Gamification service

- **Code**: [backEnd/Microservices/gamification](../../backEnd/Microservices/gamification)
- **Spring name**: `gamification`
- **Port**: `8088`
- **Gateway**: `http://localhost:8078/gamification/**`

## Responsibilities

**Achievements**, **user achievements**, **levels**, and gamification rules:

- **AchievementController**, **UserAchievementController**
- **GamificationController**, **UserLevelController**

## Data

- **MySQL** `gamificationdb`

## Run

```bash
cd backEnd/Microservices/gamification
mvn spring-boot:run
```

## API docs

If enabled: `http://localhost:8078/gamification/swagger-ui.html`

## See also

- [services-and-ports.md](../services-and-ports.md)
