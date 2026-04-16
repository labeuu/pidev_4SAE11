# Subcontracting service

## Overview

- **Folder**: `backEnd/Microservices/Subcontracting`
- **Application name**: `SUBCONTRACTING`
- **Port**: `8110` (from Config Server)
- **Gateway prefix**: `/subcontracting`
- **Primary datastore**: MySQL `gestion_subcontracting_db`

This service handles subcontracting workflows and communicates with other services (notably user, project, and notification) through Feign clients.

## Configuration

- Uses Config Server bootstrap: `spring.config.import=configserver:http://localhost:8888`
- Main shared config file: `backEnd/ConfigServer/src/main/resources/config/SUBCONTRACTING.properties`
- Registers with Eureka (`http://localhost:8420/eureka`)

## Local run

1. Start Eureka (`8420`)
2. Start Config Server (`8888`)
3. Start Subcontracting service (`8110`)
4. Access through Gateway: `http://localhost:8078/subcontracting/...`

## Related docs

- [services-and-ports.md](../services-and-ports.md)
- [api-gateway.md](../api-gateway.md)
