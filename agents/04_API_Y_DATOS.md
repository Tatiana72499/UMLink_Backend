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
GET    /api/projects
GET    /api/projects/{id}
POST   /api/projects

GET    /api/projects/{projectId}/diagrams
POST   /api/projects/{projectId}/diagrams
GET    /api/diagrams/{diagramId}

POST   /api/diagrams/{diagramId}/classes
PUT    /api/classes/{id}
DELETE /api/classes/{id}

POST   /api/classes/{classId}/attributes
DELETE /api/attributes/{id}

POST   /api/diagrams/{diagramId}/relations
DELETE /api/relations/{id}
```

## Entidades persistidas

```text
Project → Diagram → UmlClass → UmlAttribute
                  └→ UmlRelation
```

Una relación UML debe enlazar dos clases del mismo diagrama. Esta regla se valida en el service.

## Colaboración

- Endpoint WebSocket: `/ws`.
- Publicación desde cliente: `/app/diagram-events`.
- Suscripción: `/topic/diagram-events`.
- Los eventos deben tener `diagramId`, `type` y `payload`.
