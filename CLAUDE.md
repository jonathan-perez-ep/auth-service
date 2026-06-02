# auth-service

Servidor de autorización OAuth2 que emite tokens JWT. Construido sobre Spring Authorization Server, persistiendo clientes y tokens en PostgreSQL.

## Stack

- Java 17
- Spring Boot 4.0.6
- Spring Security + Spring Authorization Server
- Spring Data JPA + PostgreSQL
- Lombok
- Maven (wrapper incluido)

## Paquete base

`ep.example.auth`

## Comandos principales

```bash
# Compilar y empaquetar (sin tests)
./mvnw clean package -DskipTests

# Ejecutar la aplicación
./mvnw spring-boot:run

# Ejecutar todos los tests
./mvnw test

# Ejecutar una clase de test específica
./mvnw test -Dtest=NombreDelTest

# Solo compilar (feedback rápido)
./mvnw compile
```

En Windows usar `mvnw.cmd` en lugar de `./mvnw`.

## Configuración

Archivo principal: `src/main/resources/application.yaml`

Propiedades mínimas para levantar la aplicación:

```yaml
spring:
  application:
    name: auth-service
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db
    username: <usuario>
    password: <contraseña>
  jpa:
    hibernate:
      ddl-auto: create   # usar 'create' solo en desarrollo inicial
    show-sql: false
```

## Base de datos

PostgreSQL. Spring Authorization Server requiere las siguientes tablas (definidas en `oauth2-authorization-schema.sql` incluido en la dependencia):

- `oauth2_registered_client`
- `oauth2_authorization`
- `oauth2_authorization_consent`

Ejecutar los scripts del jar de Spring Authorization Server antes del primer arranque, o usar `ddl-auto: create` temporalmente.

## Endpoints OAuth2 (rutas por defecto)

| Endpoint | Ruta |
|---|---|
| Autorización | `/oauth2/authorize` |
| Token | `/oauth2/token` |
| Introspección de token | `/oauth2/introspect` |
| Revocación de token | `/oauth2/revoke` |
| JWK Set | `/oauth2/jwks` |
| OIDC UserInfo | `/userinfo` |
| OIDC Discovery | `/.well-known/openid-configuration` |

## Estructura del proyecto (planificada)

```
src/main/java/ep/example/auth/
├── AuthServiceApplication.java
├── config/          # SecurityConfig, AuthorizationServerConfig, JpaConfig
├── domain/          # Entidades User, Role
├── repository/      # Repositorios JPA
├── service/         # Implementación de UserDetailsService
└── web/             # Endpoints personalizados si se necesitan
```

## Testing

Las dependencias de test incluyen `spring-security-test`, `spring-boot-starter-test` y el slice de test para resource server OAuth2. Los tests viven en `src/test/java/ep/example/auth/`.

## Notas

- Sin archivos Docker aún — ejecutar PostgreSQL localmente o con un `docker-compose.yml` aparte.
- Sin herramienta de migración SQL (Flyway/Liquibase) configurada; agregar una antes de ir a producción.
- Lombok está configurado vía annotation processor en `maven-compiler-plugin` — no requiere plugin del IDE para compilar, pero sí para editar cómodamente.
