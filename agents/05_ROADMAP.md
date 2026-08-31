# Roadmap priorizado

## Fase 1 — calidad del núcleo backend

- Añadir pruebas unitarias y de integración para proyectos y diagramas.
- Completar actualización/eliminación de proyectos y diagramas.
- Manejar errores de validación y conflictos de versión de forma uniforme.
- Documentar API con OpenAPI/Swagger.

## Fase 2 — colaboración

- Persistir eventos de diagrama.
- Difundir cambios por WebSocket por cada diagrama.
- Implementar control de versiones y respuesta `409 Conflict`.
- Agregar usuarios, miembros de proyecto y roles.

## Fase 3 — interoperabilidad e IA

- Importar/exportar un subconjunto documentado de XML UML.
- Definir comandos estructurados: crear clase, atributo, relación, eliminar y mover.
- Validar y ejecutar comandos IA mediante services existentes.

## Fase 4 — generación y móvil

- Generar backend Spring Boot en capas desde el modelo UML.
- Crear migraciones PostgreSQL del backend generado.
- Crear app móvil con lectura/escritura y sincronización offline.

No iniciar una fase posterior si la fase actual no cumple los estándares de `02_CALIDAD_OBLIGATORIA.md`.
