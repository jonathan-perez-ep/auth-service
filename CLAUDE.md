# auth-service

Servidor de autorización OAuth2 que emite tokens JWT. Construido sobre Spring Authorization Server, persistiendo clientes y tokens en PostgreSQL.

## Stack

- Java 17
- Spring Boot 4.0.6 (Spring Security 7.0.5, Spring Framework 7.0.7)
- Spring Authorization Server (incluido en `spring-security-config` desde Spring Security 7.x)
- Spring Data JPA + PostgreSQL
- Flyway 11.x (configurado manualmente — Spring Boot 4.x no incluye auto-configuración)
- Spring Mail + Mailtrap (sandbox para desarrollo)
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

PostgreSQL local (`auth_db`). Las tablas las crea Flyway al arrancar desde `src/main/resources/db/migration/`.

| Tabla | Propósito |
|---|---|
| `oauth2_registered_client` | Apps autorizadas a pedir tokens |
| `oauth2_authorization` | Historial de tokens emitidos |
| `oauth2_authorization_consent` | Consentimientos aprobados por usuarios |
| `users` | Usuarios del sistema |
| `confirmation_tokens` | Tokens para confirmación de cuenta |
| `flyway_schema_history` | Historial de migraciones aplicadas |

Al arrancar se inserta automáticamente el cliente `demo-client` si no existe.

## Variables de entorno

Copiar `.env.example` como `.env` y completar los valores. El `.env` nunca se sube al repositorio.

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_USERNAME` | Sí | Usuario de PostgreSQL |
| `DB_PASSWORD` | Sí | Contraseña de PostgreSQL |
| `DB_HOST` | No (default: localhost) | Host de PostgreSQL |
| `DB_PORT` | No (default: 5432) | Puerto de PostgreSQL |
| `DB_NAME` | No (default: auth_db) | Nombre de la BD |
| `ISSUER_URI` | No (default: http://localhost:9000) | URL base del AS |
| `SERVER_PORT` | No (default: 9000) | Puerto del servidor |
| `MAIL_USERNAME` | Sí | Usuario SMTP (Mailtrap en dev) |
| `MAIL_PASSWORD` | Sí | Contraseña SMTP |
| `MAIL_HOST` | No (default: sandbox.smtp.mailtrap.io) | Host SMTP |
| `MAIL_PORT` | No (default: 2525) | Puerto SMTP |
| `MAIL_FROM` | No (default: noreply@auth-service.com) | Remitente de emails |

## Estructura del proyecto

```
src/main/java/ep/example/auth/
├── AuthServiceApplication.java
├── config/                              # configuraciones globales Spring
│   ├── AuthorizationServerConfig.java  # endpoints OAuth2, clientes, llaves RSA
│   ├── SecurityConfig.java             # seguridad HTTP, CSRF, rutas públicas
│   ├── FlywayConfig.java               # configuración manual de Flyway
│   ├── DataInitializer.java            # inserta usuario de prueba al arrancar
│   └── UserDetailsServiceImpl.java     # carga usuarios para autenticación
├── domain/                              # entidades JPA
│   ├── User.java
│   ├── UserRoleEnum.java               # roles: USER, ADMIN
│   └── ConfirmationToken.java
├── infrastructure/                      # repositorios JPA base (solo métodos estándar)
│   ├── UserRepository.java
│   └── ConfirmationTokenRepository.java
├── features/                            # features de negocio
│   └── auth/
│       ├── register/                    # POST /auth/register
│       │   ├── RegisterController.java
│       │   ├── RegisterRequest.java
│       │   └── RegisterService.java
│       └── confirm/                     # GET /auth/confirm
│           ├── ConfirmController.java
│           └── ConfirmService.java
└── shared/                              # utilidades compartidas entre features
    └── email/
        └── EmailService.java

src/main/resources/
├── application.yaml
└── db/migration/
    └── V{YYYYMMDDHHMMSS}__{descripcion}.sql  # formato timestamp — ver historial: SELECT * FROM flyway_schema_history
```

## Convenciones del proyecto

### Arquitectura

- Paquetes organizados por feature: `features/auth/{nombre-feature}/`
- Nombres de features en infinitivo o sustantivo corto: `register`, `confirm`, `password-reset`
- Cada feature contiene solo sus propias clases — sin interfaces para Services
- DTOs de entrada nombrados `{Feature}Request`, de salida `{Feature}Response`
- Repositorios JPA en `infrastructure/` solo con métodos estándar de Spring Data
- Utilidades compartidas entre features en `shared/`

### Commits

- Idioma: siempre en español
- Formato: `tipo: descripción` (minúsculas, sin punto final, máx. 70 caracteres)
- Tipos: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`, `style`

### Tests

- Services → tests unitarios con Mockito (`{Class}Test.java`)
- Controllers → tests de integración con MockMvc (`{Class}IntegrationTest.java`)
- Nombres de métodos en inglés: `methodName_withCondition_expectedBehavior()`
- NO `@Transactional` en tests de integración — usar `@BeforeEach` para limpiar datos
- Borrar primero tablas hijas (FK) y luego tablas padre en el cleanup

## Skills disponibles

Skills propios en `.claude/skills/`. Invocar con `/nombre-skill`.

| Skill | Descripción |
|---|---|
| `/generate-tests` | Genera tests unitarios (Service) o de integración (Controller) |
| `/new-feature` | Scaffold completo: Controller + Service + Request DTO + migración opcional |
| `/new-migration` | Crea migración Flyway con timestamp correcto y estilo SQL del proyecto |
| `/commit` | Genera mensaje de commit en español, propone y ejecuta con push opcional |

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

## Endpoints de la aplicación

| Endpoint | Método | Acceso | Descripción |
|---|---|---|---|
| `/auth/register` | POST | Público | Registro de nuevos usuarios |
| `/auth/confirm` | GET | Público | Confirmación de cuenta por token |

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

// CSRF deshabilitado para endpoints REST /auth/**
http.csrf(csrf -> csrf.ignoringRequestMatchers("/auth/**"))
```

## Tests

BD de tests separada: `auth_db_test` (PostgreSQL local). Configuración en `src/test/resources/application-test.yaml`.

```bash
# Correr todos los tests
.\mvnw.cmd test

# Correr solo los tests de integración del AS
.\mvnw.cmd test -Dtest=AuthorizationServerIntegrationTest
```

Los tests usan `ddl-auto: none` — Flyway crea las tablas al iniciar el contexto de test.

Usuario de prueba disponible en `auth_db`: `user` / `password` (creado por `DataInitializer` al arrancar).

## Notas generales

- Las llaves RSA se generan en memoria al arrancar — los tokens emitidos se invalidan al reiniciar. En producción deben persistirse.
- Flyway no tiene auto-configuración en Spring Boot 4.x — ver `FlywayConfig.java` y el `@DependsOn("flyway")` en `AuthorizationServerConfig`.
- Docker: `Dockerfile` (multi-stage build) + `docker-compose.yml` (solo PostgreSQL para devs sin instalación local). El backend se corre desde VS Code contra el postgres local o el containerizado.
- Para levantar solo postgres: `docker compose up -d`
- Usuarios no confirmados (`enabled=false`) son rechazados por Spring Security con `DisabledException`.
