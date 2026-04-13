# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline --no-transfer-progress -q

COPY src ./src
RUN mvn package -DskipTests --no-transfer-progress -q

# Stage 2: Runtime (minimal JRE image)
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd -g 1001 appgroup && useradd -u 1001 -g appgroup -m -s /bin/sh appuser
COPY --from=builder /build/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8092

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
