# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml .
RUN set -eux; \
    success=0; \
    for i in 1 2 3; do \
      if mvn dependency:go-offline --no-transfer-progress; then \
        success=1; \
        break; \
      fi; \
      echo "Maven go-offline failed (attempt $i), retrying..."; \
      sleep 5; \
    done; \
    test "$success" -eq 1

COPY src ./src
RUN mvn package -DskipTests --no-transfer-progress -q

# Stage 2: Runtime (minimal JRE image)
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd -g 1001 appgroup && useradd -u 1001 -g appgroup -m -s /bin/sh appuser
COPY --from=builder /build/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

# Calendar credentials are injected at runtime via a mounted volume
RUN mkdir -p /credentials && chown appuser:appgroup /credentials
USER appuser

EXPOSE 8101

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
