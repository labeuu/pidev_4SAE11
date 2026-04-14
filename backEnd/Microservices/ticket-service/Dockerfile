FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline --no-transfer-progress -q

COPY src ./src
RUN mvn clean package -DskipTests --no-transfer-progress -q

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd -g 1001 appgroup && useradd -u 1001 -g appgroup -m -s /bin/sh appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8096

ENTRYPOINT ["java", "-jar", "app.jar"]
