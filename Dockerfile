# Stage 1 — Compilar y empaquetar con Maven
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copiar wrapper y pom primero para aprovechar el cache de capas de Docker
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline -q

# Copiar el código y construir el JAR
COPY src src
RUN ./mvnw clean package -DskipTests -q

# Stage 2 — Imagen final solo con el JRE y el JAR
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "app.jar"]
