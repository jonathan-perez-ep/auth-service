# Plan de trabajo — `auth-server` (Authorization Server OAuth 2.0 / OIDC)

> Microservicio centralizado de autenticación y autorización para todo el ecosistema de proyectos.
> Stack: Java 21 · Spring Boot 3.x · Spring Authorization Server · PostgreSQL.
> Documento de referencia y plan de ejecución (mapeable a Trello: cada épica = lista, cada tarea = tarjeta).

---

## 1. Estado final del microservicio (cómo quedará)

### 1.1 Qué es y qué no es

`auth-server` es el **único punto de emisión de identidad** del ecosistema. Su responsabilidad es exclusivamente:

- Autenticar usuarios y máquinas.
- Emitir, refrescar y revocar tokens (JWT firmados).
- Exponer las llaves públicas (JWKS) para que los demás backends validen tokens por sí mismos.

**No** contiene lógica de negocio, **no** expone datos de dominio, y **no** es consultado en cada request (los Resource Servers validan el JWT localmente).

### 1.2 Stack tecnológico final

| Capa | Tecnología |
|---|---|
| Lenguaje / runtime | Java 21 (LTS) |
| Framework | Spring Boot 3.3+ |
| OAuth2 / OIDC | Spring Authorization Server 1.3+ |
| Seguridad | Spring Security 6 |
| Persistencia | Spring Data JPA + PostgreSQL 16 |
| Migraciones | Flyway |
| Pool de conexiones | HikariCP (default) |
| Hash de contraseñas | BCrypt (o Argon2 si se requiere extra) |
| Documentación API | springdoc-openapi (Swagger UI) |
| Observabilidad | Actuator + Micrometer + Prometheus + OpenTelemetry |
| Logging | Logback en formato JSON |
| Testing | JUnit 5 + Testcontainers + Spring Security Test |
| Build | Maven (o Gradle) |
| Empaquetado | Docker (imagen multi-stage) |
| CI/CD | GitHub Actions / GitLab CI |
| Secretos | Variables de entorno + (Vault / Secret Manager en prod) |

### 1.3 Endpoints finales expuestos

| Endpoint | Origen | Propósito |
|---|---|---|
| `GET /.well-known/openid-configuration` | Spring AS | Discovery OIDC |
| `GET /oauth2/jwks` | Spring AS | Llaves públicas para validar JWT |
| `GET /oauth2/authorize` | Spring AS | Inicio de flujo authorization_code |
| `POST /oauth2/token` | Spring AS | Emisión de access/refresh token |
| `POST /oauth2/revoke` | Spring AS | Revocación de token |
| `POST /oauth2/introspect` | Spring AS | Introspección de token |
| `GET /userinfo` | Spring AS (OIDC) | Claims del usuario autenticado |
| `GET /login` | Custom | Pantalla de login (form) |
| `POST /admin/clients/**` | Custom | CRUD de clientes (protegido) |
| `GET /actuator/health` | Actuator | Liveness / readiness |
| `GET /actuator/prometheus` | Actuator | Métricas |

### 1.4 Estructura del repositorio (estado final)

```
auth-server/
├── src/main/java/com/empresa/authserver/
│   ├── AuthServerApplication.java
│   ├── config/
│   │   ├── AuthorizationServerConfig.java
│   │   ├── SecurityConfig.java
│   │   ├── JwksKeyConfig.java
│   │   ├── CorsConfig.java
│   │   └── TokenCustomizerConfig.java
│   ├── client/
│   │   ├── ClientAdminController.java
│   │   ├── ClientService.java
│   │   └── dto/
│   ├── identity/
│   │   ├── AppUser.java
│   │   ├── AppUserRepository.java
│   │   ├── AppUserDetailsService.java
│   │   ├── Role.java
│   │   └── PasswordResetService.java
│   ├── audit/
│   │   ├── AuthEventListener.java
│   │   └── AuditLogRepository.java
│   ├── security/
│   │   ├── BruteForceProtectionService.java
│   │   └── RateLimitFilter.java
│   └── support/
│       └── exception/
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   ├── db/migration/        (Flyway: V1__init.sql, V2__...)
│   ├── templates/login.html
│   └── logback-spring.xml
├── src/test/java/...
│   ├── integration/ (Testcontainers)
│   └── unit/
├── docs/
│   ├── adr/                 (Architecture Decision Records)
│   ├── client-onboarding.md
│   └── runbook.md
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
├── .github/workflows/ci.yml
├── pom.xml
└── README.md
```

### 1.5 Modelo de datos final

Tablas de Spring Authorization Server (esquema oficial):
- `oauth2_registered_client` — clientes registrados
- `oauth2_authorization` — autorizaciones / tokens activos
- `oauth2_authorization_consent` — consentimientos

Tablas propias:
- `app_user` — usuarios (id, username, email, password_hash, enabled, locked, mfa_enabled, created_at)
- `app_role` — roles
- `app_user_role` — relación usuario–rol
- `auth_audit_log` — eventos de autenticación (login ok/fallido, emisión de token, revocación)
- `login_attempt` — control de fuerza bruta
- `password_reset_token` — tokens de recuperación de contraseña

---

## 2. Decisiones que debes tomar antes de empezar (ADRs)

Estas afectan el diseño. Recomiendo documentar cada una como un ADR en `docs/adr/`.

| # | Decisión | Opciones | Recomendación |
|---|---|---|---|
| D1 | **¿Dónde viven los usuarios hoy?** | — | ✅ RESUELTO: greenfield. El `auth-server` es dueño de la identidad. Sin migración. Requiere bootstrap de un usuario admin inicial |
| D2 | Base de datos | PostgreSQL / MySQL | PostgreSQL |
| D3 | Tipo de token | JWT autocontenido / opaco con introspección | JWT (RS256). Evita llamadas extra por request |
| D4 | ¿Login social? (Google, Microsoft) | Sí / No / Después | Dejar arquitectura lista, activar después |
| D5 | ¿MFA / segundo factor? | Sí / No / Fase 2 | Diseñar tabla `mfa`, implementar en fase posterior |
| D6 | ¿Multi-tenant? | Un tenant / varios | Por ahora: un `client_id` por proyecto (no multi-tenancy formal) |
| D7 | Despliegue | — | ✅ RESUELTO: Kubernetes |
| D8 | Gestión de secretos en prod | Vault / Secret Manager cloud / env cifrado | Según proveedor de nube |
| D9 | Rotación de llaves de firma | Manual / automatizada | JWKS con múltiples `kid`, rotación programada |

---

## 3. Plan de trabajo por épicas

> Estimaciones en *story points* (Fibonacci) como referencia de tamaño relativo, no horas.
> Cada tarea incluye **criterio de aceptación** (DoD = Definition of Done).

---

### ÉPICA 0 — Diseño y decisiones (ADRs)  · ~5 pts

| Tarea | DoD |
|---|---|
| 0.1 Resolver decisiones D1–D9 | Cada decisión escrita como ADR en `docs/adr/` |
| 0.2 Definir matriz de clientes y scopes | Tabla: por cada app (Angular, Kotlin, backend externo) → grant type, scopes, redirect URIs |
| 0.3 Definir política de tokens | TTL de access token, TTL de refresh token, rotación de refresh, política de revocación |
| 0.4 Diagrama de arquitectura del ecosistema | Diagrama aprobado por el equipo |

---

### ÉPICA 1 — Scaffolding e infraestructura base  · ~8 pts

| Tarea | DoD |
|---|---|
| 1.1 Generar proyecto (Spring Initializr) con dependencias base | El proyecto compila y arranca |
| 1.2 Configurar perfiles `dev` / `prod` y `application.yml` externalizado | Variables sensibles fuera del código |
| 1.3 `docker-compose.yml` con PostgreSQL local | `docker compose up` levanta DB lista |
| 1.4 Configurar Flyway | Migración `V1__init.sql` se aplica al arrancar |
| 1.5 Estructura de paquetes y convenciones | Paquetes creados; README inicial |
| 1.6 Configurar logging JSON (logback) | Logs estructurados con correlation id |

---

### ÉPICA 2 — Persistencia y migraciones  · ~8 pts

| Tarea | DoD |
|---|---|
| 2.1 Migración del esquema oficial de Spring AS (3 tablas) | Tablas creadas vía Flyway |
| 2.2 Migración de tablas de identidad (`app_user`, `app_role`, etc.) | Tablas + índices creados |
| 2.3 Migración de tablas de auditoría y seguridad | `auth_audit_log`, `login_attempt`, `password_reset_token` |
| 2.4 Configurar HikariCP (tamaño de pool, timeouts) | Pool configurado y probado bajo carga básica |
| 2.5 Seeds iniciales (rol admin, cliente de prueba) | Datos semilla mediante migración `R__seed` o script controlado |

---

### ÉPICA 3 — Gestión de identidad (usuarios)  · ~13 pts

| Tarea | DoD |
|---|---|
| 3.1 Entidades `AppUser`, `Role` + repositorios | Persistencia funcional con tests |
| 3.2 `AppUserDetailsService` (carga usuario + roles) | Autenticación contra DB funciona |
| 3.3 Encoder de contraseñas (BCrypt) | Contraseñas hasheadas; nunca en claro |
| 3.4 Estados de cuenta (enabled, locked, expired) | Cuenta bloqueada no puede autenticarse |
| 3.5 Bootstrap de usuario admin inicial + flujo de alta de usuarios | Admin creado de forma segura al primer arranque; endpoint/flujo de alta con validación |
| 3.6 Flujo de recuperación de contraseña | Token temporal + cambio seguro |
| 3.7 (Opcional / Fase 2) Estructura para MFA | Tablas y puntos de extensión listos |

---

### ÉPICA 4 — Authorization Server core (OAuth2 / OIDC)  · ~13 pts

| Tarea | DoD |
|---|---|
| 4.1 `AuthorizationServerConfig` con endpoints OIDC | Discovery endpoint responde correctamente |
| 4.2 Generación de par de llaves RSA + `JwksKeyConfig` | `/oauth2/jwks` expone la llave pública. ⚠️ En K8s la llave **debe compartirse entre réplicas** (persistida en DB o `Secret`), nunca generada en memoria por pod |
| 4.3 Habilitar grant `authorization_code` + PKCE | Flujo completo probado con cliente público |
| 4.4 Habilitar grant `client_credentials` | Backend externo obtiene token máquina-a-máquina |
| 4.5 Habilitar `refresh_token` | Refresh funciona y respeta TTL |
| 4.6 Pantalla de login personalizada | Login form propio integrado con el flujo |
| 4.7 Endpoint `/userinfo` con scope `openid` | Devuelve claims correctos |

---

### ÉPICA 5 — Gestión de clientes  · ~8 pts

| Tarea | DoD |
|---|---|
| 5.1 `JdbcRegisteredClientRepository` (clientes en DB) | Clientes persistidos y cargados desde DB |
| 5.2 Registrar clientes iniciales (Angular, Kotlin, backend externo) | Cada app tiene su `client_id` y scopes |
| 5.3 API de administración de clientes (`/admin/clients`) | CRUD protegido por rol admin |
| 5.4 Hash de `client_secret` | Secrets nunca almacenados en claro |
| 5.5 Documentar proceso de alta de un cliente nuevo | `docs/client-onboarding.md` |

---

### ÉPICA 6 — Seguridad y hardening  · ~13 pts

| Tarea | DoD |
|---|---|
| 6.1 Forzar HTTPS / TLS | Tráfico cifrado en todos los entornos no locales |
| 6.2 Configuración CORS para la SPA Angular | Solo orígenes permitidos |
| 6.3 Cabeceras de seguridad (HSTS, X-Content-Type, etc.) | Cabeceras presentes en respuestas |
| 6.4 Protección de fuerza bruta + bloqueo de cuenta | Tras N intentos fallidos, cuenta bloqueada temporalmente |
| 6.5 Rate limiting en `/oauth2/token` y `/login` | Peticiones excesivas rechazadas |
| 6.6 Externalización de secretos (sin secrets en repo) | `git-secrets` / revisión; secrets vía env/Vault |
| 6.7 Política de contraseñas (longitud, complejidad) | Validación al crear/cambiar contraseña |

---

### ÉPICA 7 — Tokens: customización, rotación y revocación  · ~8 pts

| Tarea | DoD |
|---|---|
| 7.1 `TokenCustomizer`: claims personalizados (roles, proyecto, tenant) | JWT incluye claims requeridos por los Resource Servers |
| 7.2 Rotación de refresh tokens + detección de reuso | Reuso de refresh token revoca la cadena |
| 7.3 Revocación de tokens (`/oauth2/revoke`) | Token revocado deja de ser válido |
| 7.4 Rotación de llaves de firma (múltiples `kid` en JWKS) | Rotación sin downtime; tokens viejos siguen validándose hasta expirar |

---

### ÉPICA 8 — Observabilidad y auditoría  · ~8 pts

| Tarea | DoD |
|---|---|
| 8.1 Spring Boot Actuator (health, info, metrics) | Endpoints de salud activos |
| 8.2 Métricas Micrometer → Prometheus | Métricas de logins, tokens emitidos, errores |
| 8.3 Trazabilidad distribuida (OpenTelemetry) + correlation id | Trazas con id propagado |
| 8.4 Auditoría de eventos de autenticación | Login ok/fallido, emisión y revocación registrados en `auth_audit_log` |
| 8.5 Dashboard básico (Grafana) | Panel con KPIs de seguridad |

---

### ÉPICA 9 — Testing  · ~13 pts

| Tarea | DoD |
|---|---|
| 9.1 Tests unitarios de servicios (identidad, clientes, seguridad) | Cobertura objetivo acordada (p. ej. >80% en core) |
| 9.2 Tests de integración con Testcontainers (PostgreSQL real) | Suite verde contra DB real efímera |
| 9.3 Tests de flujos OAuth2 (authorization_code, client_credentials, refresh) | Cada grant validado end-to-end |
| 9.4 Tests de seguridad (acceso denegado, token inválido, expirado) | Casos negativos cubiertos |
| 9.5 Test de contrato para Resource Servers | Resource server valida JWT emitido por el auth-server |

---

### ÉPICA 10 — Integración de Resource Servers  · ~8 pts

| Tarea | DoD |
|---|---|
| 10.1 Guía de configuración de Resource Server (`jwk-set-uri`) | Backend valida JWT localmente |
| 10.2 Migrar el backend Java actual a Resource Server | Backend principal protegido por el auth-server |
| 10.3 Mapeo de claims → autoridades de Spring Security | `@PreAuthorize` / `hasRole` funcionan |
| 10.4 (Opcional) Librería compartida de configuración de seguridad | Nuevos microservicios la importan y quedan protegidos |
| 10.5 Documentar integración para futuros proyectos | Plug & play documentado |

---

### ÉPICA 11 — DevOps / CI/CD / despliegue en Kubernetes  · ~18 pts

| Tarea | DoD |
|---|---|
| 11.1 Dockerfile multi-stage optimizado | Imagen ligera y reproducible |
| 11.2 Pipeline CI (build, test, análisis estático) | Pipeline verde en cada PR |
| 11.3 Análisis de vulnerabilidades de dependencias (OWASP / Dependabot) | Sin vulnerabilidades críticas |
| 11.4 Manifiestos K8s / Helm chart (Deployment, Service, Ingress) | Despliegue declarativo versionado |
| 11.5 `ConfigMap` (config por entorno) + `Secret` (credenciales, llave de firma) | Sin valores sensibles en la imagen ni en el repo |
| 11.6 Probes de `liveness` y `readiness` apuntando a Actuator | El orquestador detecta el estado real del pod |
| 11.7 `graceful shutdown` + `preStop` hook | Sin requests cortados durante despliegues/escalado |
| 11.8 Estrategia de réplicas: estado en DB, llave de firma compartida | El servicio escala horizontalmente sin inconsistencias de token |
| 11.9 HorizontalPodAutoscaler (según CPU/carga) | Escalado automático configurado |
| 11.10 Pipeline CD (deploy a staging/prod vía Helm) | Despliegue automatizado y controlado |
| 11.11 TLS gestionado en Ingress (cert-manager / cert del clúster) | HTTPS terminado correctamente |

---

### ÉPICA 12 — Documentación y onboarding  · ~5 pts

| Tarea | DoD |
|---|---|
| 12.1 README completo (arranque local, variables, comandos) | Un dev nuevo arranca el proyecto sin ayuda |
| 12.2 OpenAPI / Swagger UI de endpoints administrativos | Documentación navegable |
| 12.3 Runbook operativo (`docs/runbook.md`) | Procedimientos de incidentes y rotación de llaves |
| 12.4 Guía de onboarding de clientes y resource servers | Proceso claro para nuevos proyectos |

---

### ÉPICA 13 — Go-live / producción  · ~5 pts

| Tarea | DoD |
|---|---|
| 13.1 Checklist de seguridad pre-producción | Todos los ítems verificados |
| 13.2 Prueba de carga básica en `/oauth2/token` | Soporta carga esperada sin degradación |
| 13.3 Plan de rollback | Procedimiento documentado y probado |
| 13.4 Despliegue a producción + smoke tests | Servicio operativo y verificado |

---

## 4. Secuencia recomendada (dependencias)

```
Épica 0  →  1  →  2  →  3  ┐
                          ├→  4  →  5  →  7
                          │         │
                          └→  6 ────┘
4,5,6,7  →  8  →  9  →  10  →  11  →  12  →  13
```

El camino crítico es: diseño → infraestructura → identidad → core OAuth2 → clientes → tokens → integración → despliegue. Observabilidad, seguridad y testing avanzan en paralelo a partir de la Épica 4.

---

## 5. Definition of Done global (todo el proyecto)

Una funcionalidad solo se considera terminada cuando:

1. Código revisado vía Pull Request (mínimo 1 aprobación).
2. Tests unitarios e integración pasando en CI.
3. Sin secretos en el repositorio.
4. Documentación actualizada (README / ADR / onboarding según aplique).
5. Sin vulnerabilidades críticas en dependencias.
6. Cumple los criterios de aceptación de su tarjeta.

---

## 6. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Llave de firma distinta por pod en K8s (JWKS inconsistente) | Llave RSA compartida entre réplicas (DB o `Secret`), nunca generada en memoria por pod |
| Filtración de llave de firma | Rotación de llaves, `Secret` cifrado / Vault, acceso auditado |
| Acoplar lógica de negocio al auth-server | Regla estricta: el auth-server solo emite identidad |
| Resource Servers mal configurados | Librería compartida + guía + test de contrato |
| Brecha de fuerza bruta | Rate limiting + bloqueo de cuenta + auditoría |
| Bootstrap inseguro del admin inicial | Contraseña temporal forzada a cambiar en primer login |
