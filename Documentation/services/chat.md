# Chat service

## Overview

- **Folder**: `backEnd/Microservices/Chat`
- **Application name**: `chat`
- **Port**: `8096`
- **Gateway prefix**: _No route currently configured_
- **Primary datastore**: MySQL `chatdb`

This service manages chat/messaging functionality and is registered in Eureka for discovery.

## Configuration

- Main config file: `backEnd/Microservices/Chat/src/main/resources/application.properties`
- Eureka URL: `http://localhost:8420/eureka/`
- Config Server is disabled (`spring.cloud.config.enabled=false`)

## Local run

1. Start Eureka (`8420`)
2. Start Chat service (`8096`)
3. Use direct local URL for now: `http://localhost:8096/...`

To expose chat through Gateway, add a route in `backEnd/apiGateway/src/main/resources/application.yml`.

## Related docs

- [services-and-ports.md](../services-and-ports.md)
- [api-gateway.md](../api-gateway.md)
