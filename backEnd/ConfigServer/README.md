# Config Server

**Spring Cloud Config** — serves shared `*.properties` for microservices from `src/main/resources/config/`.

- **Port**: `8888`
- **Required for**: **Offer** and **Vendor** bootstrap (they import `configserver:http://localhost:8888`)

## Documentation

- **Role in the stack**: [Documentation/backend-infrastructure.md](../../Documentation/backend-infrastructure.md)

## Run

Start **Eureka** first if your setup relies on discovery, then:

```bash
cd backEnd/ConfigServer
mvn spring-boot:run
```

## Config files

Examples: `config/OFFER.properties`, `config/VENDOR.properties`, `config/PORTFOLIO.properties`, etc.
