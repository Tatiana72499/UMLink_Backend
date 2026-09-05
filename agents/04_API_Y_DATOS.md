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
GET    /api/projects/{id}/members
POST   /api/projects/{id}/members
PUT    /api/projects/{id}/members/{memberId}
DELETE /api/projects/{id}/members/{memberId}

GET    /api/projects/{projectId}/diagrams
POST   /api/projects/{projectId}/diagrams
GET    /api/diagrams/{diagramId}
PUT    /api/diagrams/{diagramId}
DELETE /api/diagrams/{diagramId}?version={version}
POST   /api/diagrams/{diagramId}/drawings
DELETE /api/diagrams/{diagramId}/drawings/{drawingId}
DELETE /api/diagrams/{diagramId}/drawings
GET    /api/diagrams/{diagramId}/activity

POST   /api/diagrams/{diagramId}/classes
PUT    /api/classes/{id}
DELETE /api/classes/{id}

POST   /api/classes/{classId}/attributes
PUT    /api/attributes/{id}
DELETE /api/attributes/{id}

POST   /api/classes/{classId}/operations
PUT    /api/operations/{id}
DELETE /api/operations/{id}

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
- Un proyecto tiene miembros con los roles `OWNER`, `EDITOR` y `VIEWER`. El creador se registra como `OWNER`; su `ownerId` es autoritativo y su membresía siempre se normaliza como `OWNER`. Solo este rol administra integrantes y proyecto. `EDITOR` puede modificar diagramas y `VIEWER` solo puede consultarlos. Las invitaciones usan el correo de una cuenta existente y no admiten el correo del propietario, porque esa persona ya tiene acceso como `OWNER`; un enlace al proyecto no concede acceso por sí solo.
- La documentación interactiva está disponible en `/swagger-ui.html` y el contrato OpenAPI JSON en `/v3/api-docs`.

## Entidades persistidas

```text
Project → Diagram → UmlClass → UmlAttribute
                  └→ UmlRelation
```

Una relación UML debe enlazar clases del mismo diagrama. Puede enlazar una clase consigo misma para representar una relación recursiva; una clase intermedia, en cambio, siempre requiere dos clases diferentes. Estas reglas se validan en el service.

Las relaciones `ASSOCIATION`, `AGGREGATION` y `COMPOSITION` requieren cardinalidad válida en ambos extremos. Las únicas opciones permitidas son `1..1`, `0..1` y `1..*`. `GENERALIZATION` representa herencia; `REALIZATION` y `DEPENDENCY` no usan cardinalidad.

`PUT /api/relations/{id}/cardinality` recibe `sourceCardinality` y `targetCardinality`. Solo se permite para asociación, agregación y composición; no cambia las clases conectadas ni el tipo de relación.

`PUT /api/attributes/{id}` actualiza `name`, `dataType` y `visibility`. `PUT /api/relations/{id}` actualiza clases de origen/destino, tipo y cardinalidades respetando las reglas del tipo UML.

Las solicitudes de atributos aceptan únicamente: `STRING`, `INTEGER`, `LONG`, `DOUBLE`, `BOOLEAN`, `UUID`, `LOCAL_DATE` y `LOCAL_DATE_TIME`. Las clases incluyen `fillColor` hexadecimal o nulo para no usar relleno; las relaciones admiten `label` de hasta 120 caracteres únicamente en asociación, agregación, composición y dependencia.

Las operaciones UML se incluyen dentro de cada clase. Su contrato es `name`, `visibility` (`PUBLIC`, `PRIVATE` o `PROTECTED`), `returnType` (`VOID` o uno de los tipos de atributo) y hasta diez parámetros ordenados `{ name, dataType }`. Los nombres de parámetros no se pueden repetir dentro de una misma operación.

Las relaciones pueden incluir `bendX` y `bendY` no negativos por compatibilidad. `alignmentPoints` admite hasta 20 puntos ordenados `{x, y}` para enrutar manualmente una relación y se persiste como parte de esta. Una asociación puede incluir opcionalmente `associationClassId`, que debe identificar una tercera clase del mismo diagrama; no puede ser uno de sus extremos ni utilizarse en otro tipo de relación.

`POST /api/diagrams/{diagramId}/association-classes` crea de manera transaccional una asociación muchos-a-muchos y su clase intermedia. Recibe los extremos, nombre, posición y color; omite cardinalidades y responde ambos elementos ya vinculados. Si algo no es válido, no persiste una clase ni relación parcial.

Los trazos del lápiz se almacenan como rutas SVG por diagrama. `POST /api/diagrams/{diagramId}/drawings` recibe `svgPath` de hasta 12000 caracteres; cada trazo se guarda por separado y puede eliminarse individualmente o limpiarse por completo. Solo `OWNER` y `EDITOR` pueden modificarlos.

Cada mutación persistida produce un evento de actividad con la persona, la acción controlada por el servidor y la fecha. `GET /api/diagrams/{diagramId}/activity` devuelve los 50 eventos más recientes para cualquier miembro del proyecto. Las previsualizaciones de lápiz y de interacción no se guardan.

## Colaboración

- Endpoint WebSocket: `/ws`.
- Publicación desde cliente: `/app/diagram-events`.
- Suscripción: `/topic/diagrams/{diagramId}`.
- El cliente STOMP debe enviar el JWT en el encabezado `Authorization: Bearer <token>` del frame `CONNECT`.
- Los eventos tienen `diagramId`, `type`, `payload` y `actor`. El servidor completa `actor`; no confía en ese campo desde el cliente.
- El cliente puede publicar `PRESENCE_JOINED`, `PRESENCE_LEFT` y eventos efímeros de edición: `DRAWING_PREVIEW`, `DRAWING_PREVIEW_CLEARED` y `ELEMENT_INTERACTION`. Estos últimos solo los autoriza para `OWNER` y `EDITOR`, se validan y no se persisten. El servidor publica `DIAGRAM_CHANGED` después de persistir una mutación UML; su `payload` describe la acción realizada con texto controlado por el servidor.
- Cada publicación valida que la persona autenticada sea miembro del proyecto del diagrama. La presencia está disponible para cualquier miembro; las mutaciones REST siguen restringidas por rol.
