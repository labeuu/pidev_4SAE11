# FreelanciaJob service

## Overview

- **Folder**: `backEnd/Microservices/FreelanciaJob`
- **Application name**: `FreelanciaJob`
- **Port**: `8092` (default via `SERVER_PORT`)
- **Gateway prefix**: _No route currently configured_
- **Primary datastore**: MySQL `freelancia_job_db`

This service appears to manage freelance job domain operations (job posting/matching related flows).

## Configuration

- Config import: `optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}`
- Registers with Eureka (`http://localhost:8420/eureka/`)
- Main config file: `backEnd/Microservices/FreelanciaJob/src/main/resources/application.properties`

## Local run

1. Start Eureka (`8420`)
2. Start Config Server (`8888`) if you rely on externalized config
3. Start service on `8092`
4. Use direct local URL for now: `http://localhost:8092/...`

To expose it via Gateway, add an explicit route in `backEnd/apiGateway/src/main/resources/application.yml`.

## Related docs

- [services-and-ports.md](../services-and-ports.md)
- [api-gateway.md](../api-gateway.md)
