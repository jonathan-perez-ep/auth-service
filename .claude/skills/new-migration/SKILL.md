---
description: Crea una migración Flyway con el timestamp correcto y el estilo SQL del proyecto. Soporta CREATE TABLE, ALTER TABLE, CREATE INDEX e INSERT de datos seed.
---

El usuario quiere crear una migración Flyway. Sigue estos pasos en orden.

## 1. Lee las migraciones existentes

Antes de cualquier otra cosa:

1. Lista los archivos en `src/main/resources/db/migration/` con Glob para ver el último timestamp usado.
2. Lee el archivo de migración más reciente para respetar el estilo SQL del proyecto.

## 2. Calcula el timestamp

El formato es `YYYYMMDDHHMMSS`. Las reglas:

- Usa la fecha actual del sistema.
- Para la parte de tiempo (`HHMMSS`), toma el timestamp del archivo más reciente y súmale 1. Ejemplo: si el último es `20260602090003`, el siguiente es `20260602090004`.
- Si la fecha actual es posterior a la del último archivo, usa `{fechaActual}000000`. Ejemplo: `20260605000000`.
- Nunca uses un timestamp menor o igual a uno ya existente — Flyway rechazará la migración.

## 3. Recopila contexto

Pregunta al usuario en un solo mensaje:

1. ¿Qué tipo de migración es?
   - `CREATE TABLE` — tabla nueva
   - `ALTER TABLE` — agregar o modificar columnas en tabla existente
   - `CREATE INDEX` — índice de performance
   - `INSERT` — datos seed o de referencia
   - `DROP` — eliminar tabla o columna (advertir que es destructivo e irreversible)
   - Otro — descripción libre

2. Según el tipo, pide los detalles específicos:
   - **CREATE TABLE**: nombre de la tabla, columnas (nombre, tipo SQL, restricciones: NOT NULL, UNIQUE, DEFAULT, FK)
   - **ALTER TABLE**: nombre de la tabla, columnas a agregar o cambios a hacer
   - **CREATE INDEX**: tabla, columna(s), nombre del índice
   - **INSERT**: tabla y filas a insertar
   - **DROP**: qué eliminar (confirmar que el usuario es consciente de la irreversibilidad)

3. ¿Cómo se llama el archivo? (descripción corta en minúsculas con guiones bajos, ej: `create_password_reset_tokens`, `add_phone_to_users`)

Espera la respuesta antes de continuar.

## 4. Genera el SQL

Sigue el estilo exacto de las migraciones existentes del proyecto:

```sql
-- Estilo de referencia para CREATE TABLE:
CREATE TABLE nombre_tabla (
    id           bigserial     PRIMARY KEY,
    columna_text varchar(255)  NOT NULL,
    columna_bool boolean       NOT NULL DEFAULT false,
    fk_id        bigint        NOT NULL REFERENCES otra_tabla(id),
    nullable_col timestamp     NULL
);

CREATE INDEX idx_nombre_tabla_columna ON nombre_tabla(columna);

-- Estilo de referencia para ALTER TABLE:
ALTER TABLE nombre_tabla ADD COLUMN IF NOT EXISTS nueva_col varchar(255) NOT NULL DEFAULT '';

-- Estilo de referencia para INSERT:
INSERT INTO nombre_tabla (col1, col2) VALUES ('val1', 'val2') ON CONFLICT DO NOTHING;
```

Reglas de estilo:
- Nombres de tablas y columnas en `snake_case`
- Alinear los tipos de columna verticalmente dentro del `CREATE TABLE`
- `NOT NULL` o `NULL` siempre explícito
- `CONSTRAINT uq_{tabla}_{columna} UNIQUE (columna)` para restricciones con nombre, o `UNIQUE` inline para casos simples
- `CREATE INDEX idx_{tabla}_{columna}` después del `CREATE TABLE` si la tabla tiene columnas de búsqueda frecuente

## 5. Crea el archivo

Nombre: `V{timestamp}__{descripcion}.sql`

Ejemplo: `V20260605000000__create_password_reset_tokens.sql`

Ruta: `src/main/resources/db/migration/`

## 6. Advierte sobre la regla de oro de Flyway

Al finalizar, recuerda al usuario:

> **Nunca modifiques un archivo de migración ya aplicado.** Flyway verifica el checksum de cada archivo; si cambia, lanza `FlywayException` al arrancar. Para corregir algo ya aplicado, crea una nueva migración.
