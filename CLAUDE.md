# auth-service

Servidor de autorización OAuth2 que emite tokens JWT. Construido sobre Spring Authorization Server, persistiendo clientes y tokens en PostgreSQL.

## Stack

- Java 17
- Spring Boot 4.0.6 (Spring Security 7.0.5, Spring Framework 7.0.7)
- Spring Authorization Server (incluido en `spring-security-config` desde Spring Security 7.x)
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

La aplicación levanta en `http://localhost:9000`. El discovery document está disponible en:
`http://localhost:9000/.well-known/openid-configuration`

## Base de datos

PostgreSQL local (`auth_db`). Las tablas se crean automáticamente al arrancar:

- Tablas JPA (`users`, etc.) → creadas por Hibernate via `ddl-auto`
- Tablas OAuth2 → creadas por `schema.sql` via `spring.sql.init`

| Tabla | Propósito |
|---|---|
| `oauth2_registered_client` | Apps autorizadas a pedir tokens |
| `oauth2_authorization` | Historial de tokens emitidos |
| `oauth2_authorization_consent` | Consentimientos aprobados por usuarios |

Al arrancar se inserta automáticamente el cliente `demo-client` si no existe.

## Estructura del proyecto

```
src/main/java/ep/example/auth/
├── AuthServiceApplication.java
├── config/
│   ├── AuthorizationServerConfig.java  # endpoints OAuth2, clientes, llaves RSA
│   └── SecurityConfig.java             # login, protección general, usuarios
├── domain/          # Entidades JPA (pendiente: User)
├── repository/      # Repositorios JPA (pendiente)
└── service/         # UserDetailsService con BD (pendiente)
```

## Endpoints OAuth2

| Endpoint | Ruta |
|---|---|
| Autorización | `/oauth2/authorize` |
| Token | `/oauth2/token` |
| Introspección | `/oauth2/introspect` |
| Revocación | `/oauth2/revoke` |
| JWK Set | `/oauth2/jwks` |
| OIDC UserInfo | `/userinfo` |
| OIDC Discovery | `/.well-known/openid-configuration` |

## Notas importantes — Spring Security 7.x

En Spring Security 7.x los paquetes del Authorization Server cambiaron respecto a versiones anteriores:

```java
// Configurer (antes estaba en spring-security-oauth2-authorization-server)
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;

// Nuevo DSL en HttpSecurity
http.oauth2AuthorizationServer(configurer -> configurer.oidc(...))

// securityMatcher obligatorio — Spring Security 7 lanza excepción si dos cadenas
// de filtros interceptan "any request". Siempre restringir la cadena del AS:
http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
```

## Notas generales

- Sin herramienta de migración SQL (Flyway/Liquibase) — agregar antes de producción.
- Las llaves RSA se generan en memoria al arrancar — los tokens emitidos se invalidan al reiniciar. En producción deben persistirse.
- `UserDetailsService` actual es in-memory (usuario `user` / `password`). Pendiente migrar a BD en Parte 4.
