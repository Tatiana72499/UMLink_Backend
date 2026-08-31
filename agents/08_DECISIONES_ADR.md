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

## Plantilla para futuras ADR

```text
## ADR-XXX — Título
- Estado: propuesta | aceptada | reemplazada
- Contexto:
- Decisión:
- Alternativas consideradas:
- Consecuencias:
```
