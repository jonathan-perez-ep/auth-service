---
description: Genera el scaffold completo para una nueva feature de auth-service: Controller, Service, Request DTO opcional y migración Flyway opcional. Sigue las convenciones del proyecto automáticamente.
---

El usuario quiere crear una nueva feature. Sigue estos pasos en orden.

## 1. Determina el nombre de la feature

Si el usuario pasó un argumento, úsalo. Si no, pregúntale.

El argumento debe estar en kebab-case: `password-reset`, `change-password`, `resend-confirmation`.

La estructura de paquetes sigue el patrón `features/auth/{feature}/{usecase}/`:
- **Feature** (todo minúsculas, sin separadores): `registration`, `passwordrecovery`
- **Caso de uso** (todo minúsculas, sin separadores): `register`, `confirm`, `request`
- **Prefijo de clases** (PascalCase): `PasswordReset`, `ChangePassword`

## 2. Recopila contexto

Haz estas preguntas al usuario en un solo mensaje antes de generar cualquier archivo:

1. ¿Qué hace este endpoint? (descripción en una oración)
2. ¿Cuál es el método HTTP y la ruta? (ej: `POST /auth/password-reset`)
3. ¿Qué campos recibe el request? — solo si es POST o PUT. Para cada campo: nombre, tipo Java, y validaciones (`@NotBlank`, `@Email`, `@Size`, etc.)
4. ¿Qué errores puede lanzar el Service? — para cada uno: mensaje exacto y tipo (conflicto de unicidad → 409, estado inválido → 400)
5. ¿Necesita tabla nueva en la BD? Si sí: nombre de la tabla y columnas con sus tipos SQL.

Espera la respuesta antes de continuar.

## 3. Lee el código existente como referencia

Lee estos archivos para respetar los patrones exactos del proyecto:

- Si el endpoint es POST/PUT:
  - `src/main/java/ep/example/auth/features/auth/registration/register/RegisterController.java`
  - `src/main/java/ep/example/auth/features/auth/registration/register/RegisterService.java`
  - `src/main/java/ep/example/auth/features/auth/registration/register/RegisterRequest.java`
- Si el endpoint es GET:
  - `src/main/java/ep/example/auth/features/auth/registration/confirm/ConfirmController.java`
  - `src/main/java/ep/example/auth/features/auth/registration/confirm/ConfirmService.java`

Lee también:
- `src/main/java/ep/example/auth/config/SecurityConfig.java` — para saber dónde agregar el endpoint público

## 4. Genera los archivos

### Controller

Ubicación: `src/main/java/ep/example/auth/features/auth/{feature}/{usecase}/{Prefijo}Controller.java`

Convenciones:
- `@RestController`, `@RequestMapping("/auth")`, `@RequiredArgsConstructor`
- POST/PUT: recibe `@RequestBody @Valid {Prefijo}Request request`, retorna 201 en éxito
- GET: recibe `@RequestParam String {param}`, retorna 200 en éxito
- `catch (IllegalArgumentException ex)` → status según el tipo de error indicado por el usuario (400 o 409)

### Service

Ubicación: `src/main/java/ep/example/auth/features/auth/{feature}/{usecase}/{Prefijo}Service.java`

Convenciones:
- `@Service`, `@RequiredArgsConstructor`
- `@Transactional` si modifica la BD
- Inyectar solo los repositorios necesarios
- Lanzar `IllegalArgumentException` con el mensaje exacto para cada error

### Request DTO (solo si POST o PUT)

Ubicación: `src/main/java/ep/example/auth/features/auth/{feature}/{usecase}/{Prefijo}Request.java`

Convenciones (igual que `RegisterRequest`):
- `@Getter`, `@NoArgsConstructor`, `@AllArgsConstructor` de Lombok
- Una anotación de validación Jakarta por campo según lo indicado

### Migración Flyway (solo si necesita tabla nueva)

Ubicación: `src/main/resources/db/migration/`

Nombre: `V{YYYYMMDDHHMMSS}__{nombre_tabla}.sql`

Para el timestamp usa la fecha actual del sistema en formato `YYYYMMDD` seguido de `000000`. Ejemplo: `V20260605000000__create_password_reset_tokens.sql`

Contenido: solo el `CREATE TABLE` con las columnas indicadas por el usuario.

## 5. Actualiza SecurityConfig

Si el endpoint debe ser público (accesible sin login), agrega su ruta al `requestMatchers` existente en `SecurityConfig.java`:

```java
.requestMatchers("/auth/register", "/auth/confirm", "/auth/{nueva-ruta}").permitAll()
```

## 6. Verifica que compila

```
.\mvnw.cmd compile
```

Si hay errores, corrígelos antes de reportar como completo.

## 7. Reporta el resultado

Al terminar, muestra al usuario:
- Los archivos creados (con sus rutas)
- Si se actualizó SecurityConfig
- Si se creó migración
- El comando para correr los tests cuando los implemente: `.\mvnw.cmd test -Dtest={Prefijo}ControllerIntegrationTest`
