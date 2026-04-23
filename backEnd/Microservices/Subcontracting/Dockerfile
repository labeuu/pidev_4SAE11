FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
RUN set -eux; \
    success=0; \
    for i in 1 2 3; do \
      if mvn dependency:go-offline --no-transfer-progress -q; then \
        success=1; \
        break; \
      fi; \
      echo "Maven command failed (attempt $i), retrying..."; \
      sleep 5; \
    done; \
    test "$success" -eq 1

COPY src ./src
RUN set -eux; \
    success=0; \
    for i in 1 2 3; do \
      if mvn clean package -DskipTests --no-transfer-progress -q; then \
        success=1; \
        break; \
      fi; \
      echo "Maven command failed (attempt $i), retrying..."; \
      sleep 5; \
    done; \
    test "$success" -eq 1

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd -g 1001 appgroup && useradd -u 1001 -g appgroup -m -s /bin/sh appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8110

ENTRYPOINT ["java", "-jar", "app.jar"]
