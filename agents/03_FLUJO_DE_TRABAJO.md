# Flujo de trabajo

## Para implementar una funcionalidad

1. Leer `AGENTS.md` y los documentos de `agents/`.
2. Revisar las entidades, DTOs y endpoints afectados.
3. Definir el contrato de entrada y salida antes de escribir el controller.
4. Implementar primero repository/model si hace falta, luego service y por último controller.
5. Agregar validaciones, manejo de errores y logs relevantes.
6. Crear una migración Flyway si cambia el esquema.
7. Ejecutar `mvn test`.
8. Marcar con `[x]` en `agents/05_ROADMAP.md` cada tarea que haya quedado realmente terminada y probada. No marcar tareas parciales.
9. Informar los archivos cambiados, comportamiento y verificación realizada.

## Operaciones de riesgo

Pedir confirmación antes de:

- borrar datos o tablas;
- cambiar la configuración de PostgreSQL;
- reemplazar migraciones existentes;
- cambiar puertos, credenciales o CORS en ambientes compartidos;
- ejecutar comandos Git destructivos.

## Convenciones de commits sugeridas

```text
feat(diagram): agrega creación de clases UML
fix(diagram): valida clases dentro del diagrama
test(diagram): cubre creación de relaciones
docs: actualiza instrucciones de arquitectura
```
