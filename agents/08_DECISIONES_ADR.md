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

## ADR-008 — Flutter generado y Ollama opcional en la aplicación resultante

- **Estado:** aceptada.
- **Contexto:** el mismo modelo UML debe generar un backend y una app móvil funcional, sin convertir UMLink en un servicio de IA local ni exigir una API de pago.
- **Decisión:** UMLink genera un ZIP Flutter desacoplado, con CRUD y `API_BASE_URL` configurable hacia el backend generado. Las referencias persistentes viajan en el backend generado como IDs validados (`<recurso>Id` o `<recurso>Ids`), base para los selectores móviles. La plantilla incluye una integración de Ollama opcional y deshabilitada, configurada únicamente dentro de la aplicación resultante mediante `OLLAMA_BASE_URL` y `OLLAMA_MODEL`.
- **Alternativas consideradas:** conectar UMLink directamente a Ollama o incluir una clave/proveedor de IA dentro del generador.
- **Consecuencias:** Flutter puede usar Ollama local o un endpoint de red del usuario; desde Android `localhost` no representa la computadora de desarrollo. El modelo de IA y la voz son descargas/configuraciones separadas, no parte obligatoria del ZIP inicial. UMLink sigue sin enviar el modelo UML a Ollama ni almacenar secretos de IA.

## ADR-009 — Acceso a datos solo mediante el backend generado

- **Estado:** aceptada.
- **Contexto:** la app Flutter generada debe poder usar PostgreSQL local durante una demostración o una base administrada como Neon, sin exponer secretos en el cliente móvil.
- **Decisión:** Flutter llama únicamente a la API REST de Spring Boot con una URL configurable. El backend conserva `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`, ejecuta Flyway y se conecta a PostgreSQL. La interfaz generada ofrece el control **Servidor local** para cambiar temporalmente la URL de API sin recompilar.
- **Consecuencias:** para un teléfono físico debe utilizarse la IP LAN del equipo y permitir el puerto del backend en el firewall. La URL seleccionada se conserva solo durante la sesión actual; su persistencia formará parte del incremento offline.

## Plantilla para futuras ADR

```text
## ADR-XXX — Título
- Estado: propuesta | aceptada | reemplazada
- Contexto:
- Decisión:
- Alternativas consideradas:
- Consecuencias:
```


## ADR-010 — Operaciones UML como contratos explícitos del backend generado

- **Estado:** aceptada.
- **Contexto:** las operaciones definidas por una persona en el diagrama deben conservarse al generar el backend, pero UML no describe por sí solo la regla de negocio.
- **Decisión:** mantener CRUD para cada clase y generar por cada operación UML un endpoint POST tipado, con DTO validado si recibe parámetros, que delega a un método del service. El stub verifica la existencia de la entidad y responde 501 NOT_IMPLEMENTED hasta que se programe su implementación.
- **Consecuencias:** el código no inventa comportamientos de dominio ni aparenta que la operación esté terminada; una operación con el mismo nombre normalizado que otra de la misma clase se rechaza al generar para evitar rutas ambiguas.
