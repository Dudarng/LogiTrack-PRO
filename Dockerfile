# Estágio 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Aproveita cache do Docker para dependências copiando apenas pom.xml
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia código-fonte e compila pacote final sem executar testes
COPY src ./src
RUN mvn clean package -DskipTests -B

# Estágio 2: Runtime enxuto e seguro
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Cria usuário e grupo não-root para execução segura
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copia apenas o artefato final com permissão para o usuário da aplicação
COPY --from=builder --chown=appuser:appgroup /app/target/logitrack-pro-*.jar app.jar

USER appuser
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
