# Build stage
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies (cached layer if pom unchanged)
RUN set -eux; \
    success=0; \
    for i in 1 2 3; do \
      if ./mvnw dependency:go-offline -B; then \
        success=1; \
        break; \
      fi; \
      echo "Maven command failed (attempt $i), retrying..."; \
      sleep 5; \
    done; \
    test "$success" -eq 1

# Copy source and build
COPY src ./src
RUN set -eux; \
    success=0; \
    for i in 1 2 3; do \
      if ./mvnw clean package -DskipTests -B; then \
        success=1; \
        break; \
      fi; \
      echo "Maven command failed (attempt $i), retrying..."; \
      sleep 5; \
    done; \
    test "$success" -eq 1

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create non-root user for security
RUN groupadd -g 1001 appgroup && useradd -u 1001 -g appgroup -m -s /bin/sh appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8091

ENTRYPOINT ["java", "-jar", "app.jar"]
