# UML Collaboration Backend

Backend inicial para almacenar diagramas UML colaborativos. Usa Java 21, Spring Boot, PostgreSQL, Flyway y WebSocket/STOMP.

## Base de datos

Crear la base de datos:

```sql
CREATE DATABASE uml_collab;
```

O iniciar PostgreSQL con Docker:

```powershell
docker compose up -d
```

Configurar opcionalmente `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`. Los valores por defecto son `jdbc:postgresql://localhost:5432/uml_collab`, usuario `postgres` y contraseña `postgres`.

## Ejecutar

```powershell
mvn spring-boot:run
```

La API inicia en `http://localhost:8080` y Flyway crea el esquema automáticamente.

## Primer flujo REST

1. `POST /api/projects`
2. `POST /api/projects/{projectId}/diagrams`
3. `POST /api/diagrams/{diagramId}/classes`
4. `POST /api/classes/{classId}/attributes`
5. `POST /api/diagrams/{diagramId}/relations`

El WebSocket STOMP está disponible en `/ws`; publica eventos en `/app/diagram-events` y los clientes se suscriben a `/topic/diagram-events`.
