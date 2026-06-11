Ejecuta una revisión completa del código: primero seguridad, luego correctitud y calidad.

## 1. Security Review

Invoca el skill `security-review` con el Skill tool.

Muestra su output completo al usuario.

## 2. Code Review

Invoca el skill `code-review` con el Skill tool.

Muestra su output completo al usuario.

## 3. Resumen final

Al terminar ambos, muestra un resumen consolidado con el formato:

```
## Resumen full-review

**Seguridad** — N hallazgos
  - (lista los títulos de los hallazgos de security-review, o "Sin hallazgos")

**Calidad** — N hallazgos
  - (lista los títulos de los hallazgos de code-review, o "Sin hallazgos")

**Acción recomendada**: (crítico / revisar antes de mergear / sin bloqueos)
```
