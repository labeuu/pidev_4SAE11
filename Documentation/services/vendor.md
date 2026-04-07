# Vendor service

- **Code**: [backEnd/Microservices/Vendor](../../backEnd/Microservices/Vendor)
- **Spring name**: `VENDOR` (Eureka)
- **Port**: `8093` (from [Config Server VENDOR.properties](../../backEnd/ConfigServer/src/main/resources/config/VENDOR.properties))
- **Gateway**: `http://localhost:8078/vendor/**` (direct to `localhost:8093`)

## Responsibilities

**Vendor / agrément** workflows and admin approval flows:

- **VendorApprovalController**

## Configuration

Bootstrap imports **Config Server**: start **Eureka** and **Config Server** first. Local overrides via `application-local.properties`.

## Data

- **MySQL** `gestion_vendor_db`

## Run

```bash
cd backEnd/Microservices/Vendor
mvn spring-boot:run
```

## API docs

If enabled: `http://localhost:8078/vendor/swagger-ui.html`

## See also

- [backend-infrastructure.md](../backend-infrastructure.md)
- [services-and-ports.md](../services-and-ports.md)
