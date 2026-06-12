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
| `account_confirmation_tokens` | Tokens para confirmación de cuenta de registro |
| `password_reset_tokens` | Tokens para recuperación de contraseña |
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
| `DEMO_CLIENT_ID` | No (default: demo-client) | Client ID del cliente OAuth2 de desarrollo |
| `DEMO_CLIENT_SECRET` | Sí | Secreto del cliente OAuth2 de desarrollo |
| `RSA_PRIVATE_KEY` | No (default: genera en memoria) | Clave privada RSA Base64 PKCS8 para firmar JWT. Sin valor, los tokens se invalidan al reiniciar. |
| `RSA_PUBLIC_KEY` | No (default: genera en memoria) | Clave pública RSA Base64 X509 para verificar JWT. Generar con `jshell`. |

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
│   ├── AccountConfirmationToken.java
│   └── PasswordResetToken.java
├── infrastructure/                      # repositorios JPA base (solo métodos estándar)
│   ├── UserRepository.java
│   ├── AccountConfirmationTokenRepository.java
│   └── PasswordResetTokenRepository.java
├── features/                            # features de negocio
│   ├── auth/
│   │   ├── registration/               # feature: registro de usuarios
│   │   │   ├── register/               # POST /auth/register
│   │   │   │   ├── RegisterController.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   └── RegisterService.java
│   │   │   └── confirm/                # GET /auth/register/confirm
│   │   │       ├── RegistrationConfirmController.java
│   │   │       └── RegistrationConfirmService.java
│   │   └── passwordrecovery/           # feature: recuperación de contraseña
│   │       ├── request/                # POST /auth/password-recovery
│   │       │   ├── PasswordRecoveryController.java
│   │       │   ├── PasswordRecoveryRequest.java
│   │       │   ├── PasswordRecoveryService.java
│   │       │   └── PasswordRecoveryRepository.java  # repositorio JDBC para UPDATE bulk
│   │       └── confirm/                # POST /auth/password-recovery/confirm
│   │           ├── PasswordRecoveryConfirmController.java
│   │           ├── PasswordRecoveryConfirmRequest.java
│   │           └── PasswordRecoveryConfirmService.java
│   └── account/
│       └── changepassword/             # POST /account/change-password
│           ├── ChangePasswordController.java
│           ├── ChangePasswordRequest.java
│           └── ChangePasswordService.java
└── shared/                              # utilidades compartidas entre features
    ├── email/
    │   └── EmailService.java
    └── exception/
        ├── GlobalExceptionHandler.java  # @RestControllerAdvice centralizado
        └── ConflictException.java       # excepción para respuestas 409

src/main/resources/
├── application.yaml
└── db/migration/
    └── V{YYYYMMDDHHMMSS}__{descripcion}.sql  # formato timestamp — ver historial: SELECT * FROM flyway_schema_history
```

## Convenciones del proyecto

### Arquitectura

- Arquitectura Vertical Slice: `features/{module}/{feature}/{usecase}/`
  - **Módulo** (todo minúsculas): `auth` (flujos de autenticación pública), `account` (gestión de cuenta autenticada)
  - **Feature** (todo minúsculas): `registration`, `passwordrecovery`
  - **Caso de uso** (todo minúsculas): `register`, `confirm`, `request`
- Si el módulo tiene un único caso de uso, puede aplanarse: `features/account/changepassword/` (sin sub-paquete de feature)
- Nombres de clases incluyen contexto de feature: `RegistrationConfirmController`, `PasswordRecoveryConfirmService`
- Cada caso de uso contiene solo sus propias clases — sin interfaces para Services
- DTOs de entrada nombrados `{Feature}Request`, de salida `{Feature}Response`
- Repositorios JPA en `infrastructure/` solo con métodos estándar de Spring Data
- Cuando se necesita una operación bulk no soportada por Spring Data (ej. UPDATE masivo), crear un repositorio JDBC dentro del paquete del caso de uso — no en `infrastructure/` (ej. `PasswordRecoveryRepository`)
- Utilidades compartidas entre features en `shared/`

### Commits

- Idioma: siempre en español
- Formato: `tipo: descripción` (minúsculas, sin punto final, máx. 70 caracteres)
- Tipos: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`, `style`

## Skills disponibles

Skills propios en `.claude/skills/`. Invocar con `/nombre-skill`.

| Skill | Descripción |
|---|---|
| `/generate-tests` | Genera tests unitarios (Service) o de integración (Controller) |
| `/new-feature` | Scaffold completo: Controller + Service + Request DTO + migración opcional |
| `/new-migration` | Crea migración Flyway con timestamp correcto y estilo SQL del proyecto |
| `/commit` | Genera mensaje de commit en español, propone y ejecuta con push opcional |
| `/full-review` | Ejecuta `/security-review` y `/code-review` en secuencia con resumen consolidado |
| `/context-sync` | Audita si `CLAUDE.md` está sincronizado con el estado actual del repo |

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
| `/auth/register/confirm` | GET | Público | Confirmación de cuenta por token |
| `/auth/password-recovery` | POST | Público | Solicitar reset de contraseña |
| `/auth/password-recovery/confirm` | POST | Público | Aplicar nuevo password con token |
| `/account/change-password` | POST | Autenticado | Cambiar contraseña del usuario autenticado |

## Notas importantes — Spring Security 7.x

- El configurer del AS está en `org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer` (ya no en un artefacto separado).
- Usar `http.oauth2AuthorizationServer(configurer -> ...)` para el DSL del AS.
- `securityMatcher` obligatorio — Spring Security 7 lanza excepción si dos cadenas de filtros interceptan "any request". Siempre `http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())`.
- CSRF deshabilitado para `/auth/**` y `/account/**` — APIs REST con Bearer token no lo necesitan; el login-form sí.

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

### Convenciones

- Services → tests unitarios con Mockito (`{Class}Test.java`)
- Controllers → tests de integración con MockMvc (`{Class}IntegrationTest.java`)
- Nombres de métodos en inglés: `methodName_withCondition_expectedBehavior()`
- NO `@Transactional` en tests de integración — usar `@BeforeEach` para limpiar datos
- Borrar primero tablas hijas (FK) y luego tablas padre en el cleanup
- Mockear dependencias externas (email, etc.) con `@MockitoBean` — Spring Boot 4.x eliminó `@MockBean`
  ```java
  import org.springframework.test.context.bean.override.mockito.MockitoBean;
  @MockitoBean EmailService emailService;
  ```

## Deuda de seguridad conocida

| Severidad | Archivo | Descripción | Mitigación recomendada |
|---|---|---|---|
| Media | `RegisterService.java:31-35` | Mensajes de error distintos para username vs email permiten enumerar cuentas existentes desde el endpoint público `/auth/register` | Rate limiting por IP o CAPTCHA en el registro |

## Notas generales

- CI configurado en `.github/workflows/ci.yml` — corre los tests en cada push a `main`. No bloquea pushes directos; para bloquear merges se necesitan branch protection rules (útil solo si hay equipo).
- Llaves RSA: si `RSA_PRIVATE_KEY` y `RSA_PUBLIC_KEY` están definidas en el entorno, se cargan y los tokens sobreviven reinicios. Si no, se generan en memoria al arrancar. Generar con `jshell` (ver `.env.example`).
- Flyway no tiene auto-configuración en Spring Boot 4.x — ver `FlywayConfig.java` y el `@DependsOn("flyway")` en `AuthorizationServerConfig`.
- Docker: `Dockerfile` (multi-stage build) + `docker-compose.yml` (solo PostgreSQL para devs sin instalación local). El backend se corre desde VS Code contra el postgres local o el containerizado.
- Para levantar solo postgres: `docker compose up -d`
- Usuarios no confirmados (`enabled=false`) son rechazados por Spring Security con `DisabledException`.
