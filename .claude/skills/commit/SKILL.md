---
description: Genera el mensaje de commit en español siguiendo las convenciones del proyecto, muestra una propuesta y ejecuta el commit tras confirmación del usuario.
---

## 1. Verifica que haya cambios staged

Ejecuta `git status` y `git diff --staged --stat`.

Si no hay nada en stage:
- Informa al usuario qué archivos hay modificados sin stagear.
- Pregunta si quiere stagear todo con `git add -A` o archivos específicos.
- No continúes hasta que haya algo staged.

## 2. Analiza los cambios

Ejecuta `git diff --staged` para leer el contenido completo de los cambios staged.

También ejecuta `git log --oneline -5` para ver el estilo reciente de mensajes.

## 3. Determina el tipo de commit

Elige el prefijo que mejor describe los cambios:

| Prefijo | Cuándo usarlo |
|---|---|
| `feat:` | Nueva funcionalidad visible para el usuario o el sistema |
| `fix:` | Corrección de un bug |
| `test:` | Agregar o modificar tests |
| `refactor:` | Cambio de código sin alterar funcionalidad ni corregir bugs |
| `docs:` | Solo documentación (CLAUDE.md, comentarios, README) |
| `chore:` | Tareas de mantenimiento: dependencias, configuración, scripts |
| `style:` | Cambios de formato sin impacto funcional |

Si los cambios mezclan tipos distintos (ej: feat + test), usa el tipo dominante.

## 4. Redacta el mensaje

Reglas obligatorias:
- **Idioma**: siempre en español
- **Formato**: `tipo: descripción` — todo en minúsculas, sin punto final
- **Longitud del asunto**: máximo 70 caracteres
- **Tiempo verbal**: infinitivo o presente simple ("agregar", "corregir", "renombrar")
- **Cuerpo**: solo si los cambios son complejos o no evidentes desde el asunto — separado por línea en blanco, sin límite de longitud

Ejemplos del proyecto:
```
feat: implementar feature de registro de usuarios
fix: deshabilitar CSRF para endpoints /auth/**
test: agregar tests de integración para RegisterController
refactor: renombrar migraciones a formato timestamp
docs: actualizar CLAUDE.md con estructura por features
```

## 5. Propón el mensaje al usuario

Muestra la propuesta así:

```
Mensaje propuesto:

  tipo: descripción del commit

¿Confirmas? (puedes pedirme que lo ajuste)
```

Espera confirmación antes de ejecutar el commit. Si el usuario pide cambios, ajusta y vuelve a mostrar.

## 6. Ejecuta el commit

Una vez confirmado, ejecuta:

```
git commit -m "tipo: descripción"
```

Si hay cuerpo:
```
git commit -m "tipo: descripción" -m "Cuerpo detallado aquí."
```

Nunca uses `--no-verify`.

Confirma el resultado mostrando el hash y el mensaje del commit creado.
