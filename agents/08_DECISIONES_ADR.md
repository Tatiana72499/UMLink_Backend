# Registro de decisiones técnicas (ADR)

Las decisiones significativas deben registrarse aquí. Una ADR no tiene que ser larga; debe explicar contexto, decisión, consecuencias y fecha.

## ADR-001 — Monolito modular

- **Estado:** aceptada.
- **Decisión:** usar un único backend Spring Boot organizado por funcionalidades y capas internas.
- **Motivo:** es más viable para cuatro semanas que microservicios, mantiene despliegue simple y permite separar responsabilidades.
- **Consecuencia:** cada nueva funcionalidad debe vivir bajo `features/<nombre>/`.

## ADR-002 — PostgreSQL y Flyway

- **Estado:** aceptada.
- **Decisión:** PostgreSQL es la base de datos y Flyway controla los cambios de esquema.
- **Motivo:** el proyecto exige PostgreSQL y necesita migraciones repetibles.
- **Consecuencia:** no usar `ddl-auto=create` ni modificar migraciones aplicadas.

## ADR-003 — Servidor autoritativo para colaboración

- **Estado:** aceptada.
- **Decisión:** el servidor valida, guarda y transmite todas las operaciones de edición.
- **Motivo:** reduce complejidad frente a CRDT y es suficiente para el MVP.
- **Consecuencia:** el frontend no es fuente de verdad; conflictos se detectan mediante versión y reglas de negocio.

## ADR-004 — IA por comandos estructurados

- **Estado:** aceptada.
- **Decisión:** IA transforma voz/texto en acciones acotadas como `CREATE_CLASS` o `ADD_ATTRIBUTE`.
- **Motivo:** es verificable, segura y alcanzable en el tiempo disponible.
- **Consecuencia:** la IA no escribe directamente a la base de datos ni genera el diagrama completo.

## ADR-005 — Concurrencia optimista en proyectos y diagramas

- **Estado:** aceptada.
- **Decisión:** las actualizaciones y eliminaciones de proyectos y diagramas requieren la versión conocida por el cliente; las versiones no coincidentes responden `409 VERSION_CONFLICT`.
- **Motivo:** evita sobrescrituras silenciosas y prepara el flujo colaborativo sin introducir bloqueos pesados.
- **Consecuencia:** el frontend debe conservar y enviar la versión recibida; ante conflicto debe recargar el recurso.

## ADR-006 — IA visual mediante vista previa PlantUML confirmable

- **Estado:** aceptada.
- **Contexto:** se necesita crear una propuesta editable desde una foto sin dar a un proveedor externo acceso directo a PostgreSQL ni sobrescribir diagramas.
- **Decisión:** OpenRouter se consume desde backend con `OPENROUTER_API_KEY` local y el router `openrouter/free`. La respuesta se exige como PlantUML, se valida con el importador existente y se devuelve como vista previa no persistida. Solo una confirmación explícita del usuario crea un diagrama nuevo mediante la importación transaccional existente.
- **Consecuencias:** el proveedor puede estar sujeto a disponibilidad y límites gratuitos; nunca se mandan secretos al frontend ni se registran imágenes, claves o respuestas completas.

## ADR-007 — Backend generado como ZIP no persistido

- **Estado:** aceptada.
- **Contexto:** cada diagrama debe poder convertirse en una base Spring Boot con PostgreSQL sin introducir repositorios ni archivos arbitrarios en el servidor UMLink.
- **Decisión:** generar el proyecto en memoria y descargar un ZIP. El generador crea capas por entidad y una migración Flyway inicial. Si no hay PK UML, se agrega `UUID id`; las relaciones persistentes se traducen según cardinalidad.
- **Consecuencias:** el archivo requiere revisión humana de reglas de negocio, nombres y restricciones antes de producción. La generación no modifica el diagrama ni el sistema de archivos del servidor.

## Plantilla para futuras ADR

```text
## ADR-XXX — Título
- Estado: propuesta | aceptada | reemplazada
- Contexto:
- Decisión:
- Alternativas consideradas:
- Consecuencias:
```
