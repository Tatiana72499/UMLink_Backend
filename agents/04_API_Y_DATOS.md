# API y datos

## Configuración local

```text
DB_URL=jdbc:postgresql://localhost:5432/uml_collab
DB_USERNAME=postgres
DB_PASSWORD=postgres
PORT=8080
```

## Rutas REST actuales

```text
POST   /api/auth/register
POST   /api/auth/login

GET    /api/projects
GET    /api/projects/{id}
POST   /api/projects
PUT    /api/projects/{id}
DELETE /api/projects/{id}?version={version}

GET    /api/projects/{projectId}/diagrams
POST   /api/projects/{projectId}/diagrams
GET    /api/diagrams/{diagramId}
PUT    /api/diagrams/{diagramId}
DELETE /api/diagrams/{diagramId}?version={version}

POST   /api/diagrams/{diagramId}/classes
PUT    /api/classes/{id}
DELETE /api/classes/{id}

POST   /api/classes/{classId}/attributes
PUT    /api/attributes/{id}
DELETE /api/attributes/{id}

POST   /api/diagrams/{diagramId}/relations
POST   /api/diagrams/{diagramId}/association-classes
PUT    /api/relations/{id}
PUT    /api/relations/{id}/cardinality
DELETE /api/relations/{id}
```

## Autenticación

- `POST /api/auth/register` recibe `name`, `email` y una `password` de 6 a 8 caracteres; crea el usuario y responde un JWT.
- `POST /api/auth/login` recibe `email` y `password`; responde un JWT si las credenciales son válidas.
- Toda ruta bajo `/api` excepto `/api/auth/**` requiere `Authorization: Bearer <token>`.
- El propietario de un proyecto se resuelve desde el JWT. `POST /api/projects` solo recibe `name` y `description`; el cliente no puede elegir el propietario.
- `PUT /api/projects/{id}` recibe `name`, `description` y la `version` actual; `PUT /api/diagrams/{id}` recibe `name` y `version`. Las eliminaciones reciben `version` como parámetro de consulta. Una versión desactualizada responde `409 VERSION_CONFLICT`.
- Los errores REST tienen la forma `{ timestamp, status, code, message, fieldErrors }`. Las validaciones usan `VALIDATION_ERROR`, recursos inexistentes `RESOURCE_NOT_FOUND`, reglas de negocio `BUSINESS_RULE_VIOLATION` y concurrencia `VERSION_CONFLICT`.
- La documentación interactiva está disponible en `/swagger-ui.html` y el contrato OpenAPI JSON en `/v3/api-docs`.

## Entidades persistidas

```text
Project → Diagram → UmlClass → UmlAttribute
                  └→ UmlRelation
```

Una relación UML debe enlazar dos clases del mismo diagrama. Esta regla se valida en el service.

Las relaciones `ASSOCIATION`, `AGGREGATION` y `COMPOSITION` requieren cardinalidad válida en ambos extremos. Las únicas opciones permitidas son `1..1`, `0..1` y `1..*`. `GENERALIZATION` representa herencia; `REALIZATION` y `DEPENDENCY` no usan cardinalidad.

`PUT /api/relations/{id}/cardinality` recibe `sourceCardinality` y `targetCardinality`. Solo se permite para asociación, agregación y composición; no cambia las clases conectadas ni el tipo de relación.

`PUT /api/attributes/{id}` actualiza `name`, `dataType` y `visibility`. `PUT /api/relations/{id}` actualiza clases de origen/destino, tipo y cardinalidades respetando las reglas del tipo UML.

Las solicitudes de atributos aceptan únicamente: `STRING`, `INTEGER`, `LONG`, `DOUBLE`, `BOOLEAN`, `UUID`, `LOCAL_DATE` y `LOCAL_DATE_TIME`. Las clases incluyen `fillColor` hexadecimal o nulo para no usar relleno; las relaciones admiten `label` de hasta 120 caracteres únicamente en asociación, agregación, composición y dependencia.

Las relaciones pueden incluir `bendX` y `bendY` no negativos por compatibilidad. `alignmentPoints` admite hasta 20 puntos ordenados `{x, y}` para enrutar manualmente una relación y se persiste como parte de esta. Una asociación puede incluir opcionalmente `associationClassId`, que debe identificar una tercera clase del mismo diagrama; no puede ser uno de sus extremos ni utilizarse en otro tipo de relación.

`POST /api/diagrams/{diagramId}/association-classes` crea de manera transaccional una asociación muchos-a-muchos y su clase intermedia. Recibe los extremos, nombre, posición y color; omite cardinalidades y responde ambos elementos ya vinculados. Si algo no es válido, no persiste una clase ni relación parcial.

## Colaboración

- Endpoint WebSocket: `/ws`.
- Publicación desde cliente: `/app/diagram-events`.
- Suscripción: `/topic/diagram-events`.
- Los eventos deben tener `diagramId`, `type` y `payload`.
