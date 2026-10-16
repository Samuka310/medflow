# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY services/api-gateway ./services/api-gateway
RUN mvn clean package -DskipTests -pl services/api-gateway

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Criar grupo e usuário de sistema não-root para execução segura (CIS Benchmark)
RUN addgroup -g 10001 -S medflow && \
    adduser -u 10001 -S medflow -G medflow && \
    chown -R medflow:medflow /app

COPY --from=build --chown=medflow:medflow /app/services/api-gateway/target/*.jar app.jar

USER 10001:10001

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
