# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY libs ./libs
COPY services ./services
COPY backend ./backend
RUN mvn clean package -DskipTests -pl libs/medflow-events,backend

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Criar grupo e usuário de sistema não-root para execução segura (CIS Benchmark)
RUN addgroup -g 10001 -S medflow && \
    adduser -u 10001 -S medflow -G medflow && \
    chown -R medflow:medflow /app

COPY --from=build --chown=medflow:medflow /app/backend/target/*.jar app.jar

USER 10001:10001

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
