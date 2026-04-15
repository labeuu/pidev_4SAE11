# Meeting service

## Overview

- **Folder**: `backEnd/Microservices/Meeting`
- **Application name**: `meeting`
- **Port**: `8097` (default via `SERVER_PORT`)
- **Gateway prefix**: _No route currently configured_
- **Primary datastore**: MySQL `meetingdb`

This service supports meeting scheduling and includes optional Google Calendar/Meet integration.

## Configuration

- Main config file: `backEnd/Microservices/Meeting/src/main/resources/application.properties`
- Config import: `optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}`
- Eureka URL: `http://localhost:8420/eureka/`
- Calendar integration toggles:
  - `google.calendar.enabled`
  - `google.calendar.credentials-path`
  - `google.calendar.default-calendar-id`

## Local run

1. Start Eureka (`8420`)
2. Start Meeting service (`8101`)
3. Use direct local URL for now: `http://localhost:8097/...`

To expose meeting endpoints through Gateway, add a route in `backEnd/apiGateway/src/main/resources/application.yml`.

## Related docs

- [services-and-ports.md](../services-and-ports.md)
- [api-gateway.md](../api-gateway.md)
