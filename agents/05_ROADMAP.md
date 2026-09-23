# Roadmap priorizado del backend

Los checks reflejan únicamente elementos comprobados en el código actual.

## Fase 0 — Fundación del backend

**Estado: completada.**

- [x] Spring Boot 3 / Java 21 con arquitectura por feature: `controller`, `service`, `repository`, `model` y `dto`.
- [x] PostgreSQL y Flyway con migración inicial de proyectos, diagramas, clases, atributos y relaciones.
- [x] API REST para crear/listar/consultar proyectos y diagramas.
- [x] API REST CRUD para clases, atributos y relaciones UML.
- [x] Validación de solicitudes y manejo global básico de recursos no encontrados.
- [x] WebSocket STOMP base: recibe eventos en `/app/diagram-events` y los retransmite en `/topic/diagram-events`.

## Fase 1 — Calidad e integración del núcleo

**Estado: completada.**

- [x] Añadir pruebas HTTP para listar, crear y permitir CORS en proyectos.
- [x] Añadir pruebas HTTP para listar y crear diagramas.
- [x] Añadir pruebas unitarias y de integración para diagramas y reglas de negocio de proyectos.
- [x] Completar actualización/eliminación de proyectos y diagramas.
- [x] Manejar errores de validación y conflictos de versión de forma uniforme.
- [x] Documentar API con OpenAPI/Swagger.
- [x] Configurar CORS REST y WebSocket para el frontend Angular local (`http://localhost:4200`).

## Fase 2 — Colaboración

**Estado: iniciada solo a nivel de transporte.**

- [x] Configuración STOMP y retransmisión genérica de un evento.
- [x] Persistir eventos de diagrama: historial de las 50 mutaciones más recientes, con actor, acción y fecha.
- [x] Difundir cambios por cada diagrama, no por un tópico global.
- [x] Implementar control de versiones y respuesta `409 Conflict`.
- [x] Agregar miembros de proyecto y roles (`OWNER`, `EDITOR`, `VIEWER`) con autorización REST y WebSocket.
- [x] Compartir proyectos mediante enlace opaco de solo lectura sin crear membresía; la edición continúa requiriendo sesión y rol `EDITOR` o `OWNER`.
- [x] Persistir y difundir trazos de lápiz por diagrama para los miembros con permiso de edición.
- [x] Previsualizar trazos, resaltar clases arrastradas y comunicar actividad remota contextual mediante eventos WebSocket efímeros autorizados.

## Autenticación — base para pruebas locales

- [x] Registro e inicio de sesión con contraseñas BCrypt y JWT externo.
- [x] Asociar el creador autenticado a cada proyecto y restringir su acceso al propietario.

## Fase 3 — Interoperabilidad e IA

- [x] Importar/exportar XML UMLink completo, XMI/UML genérico, XMI 2.1 visual y script de Automation API para Enterprise Architect 15: clases, atributos, operaciones, relaciones y cardinalidades admitidas.
- [x] Validar XML/XMI de entrada, bloquear entidades externas y crear un diagrama nuevo de forma transaccional al importar.
- [x] Importar y exportar el subconjunto PlantUML de clases: atributos, una PK por clase, operaciones, colores, relaciones, cardinalidades y clases de asociación.
- [x] Persistir y exportar una única llave primaria por clase UML, sustituyendo la marca anterior al seleccionar otra.
- [x] Validar restricciones estructurales por tipo de relación: cardinalidades, etiquetas, extremos y ciclos de herencia.
- [x] Definir comandos estructurados de consulta, creación, edición, eliminación y movimiento para los elementos UML admitidos por el asistente.
- [x] Integrar la vista previa externa de IA de imagen a PlantUML mediante OpenRouter, con contrato HTTP, autenticación local, límites y confirmación explícita antes de importar.
- [x] Validar y ejecutar comandos del intérprete local mediante los services existentes, con vista previa y confirmación para mutaciones.

## Fase 4 — Generación y móvil

- [x] Generar un backend Spring Boot en capas desde el modelo UML como ZIP descargable.
- [x] Crear `V1__initial_schema.sql` de Flyway con PostgreSQL dentro del backend generado.
- [ ] Definir el contrato de transformación UML → backend Spring Boot, PostgreSQL y Flutter.
- [x] Generar una app Flutter CRUD desde las clases UML válidas, como ZIP descargable.
- [x] Generar cliente HTTP Flutter y formularios tipados para los endpoints CRUD generados, incluidos selectores para relaciones persistentes.
- [x] Ampliar los DTO y servicios del backend generado para exponer/actualizar IDs relacionados y validar que las referencias existan; después traducir relaciones persistentes a selectores y navegación Flutter.
- [ ] Incluir configuración opcional de asistente local para Ollama en la app Flutter generada, sin acoplar UMLink a Ollama ni incluir claves.
- [ ] Crear app móvil con lectura/escritura y sincronización offline.

### Contrato de generación Flutter v1

- Cada clase UML válida genera un modelo Dart, servicio REST, listado, formulario de creación/edición y acción de eliminación.
- Los atributos admitidos son `String`, `Integer`, `Long`, `Double`, `Boolean`, `UUID`, `LocalDate` y `LocalDateTime`; la PK se representa como `id` de solo lectura.
- La app generada usa `API_BASE_URL` configurable. Para emulador Android se documenta `http://10.0.2.2:8081/api`; para dispositivo físico se usa la IP local del equipo.
- El backend generado expone `<recurso>Id` o `<recurso>Ids` en sus DTO de creación, actualización y respuesta para relaciones persistentes; el service resuelve y valida esos IDs antes de guardar. Flutter los representa con un selector individual o múltiple obtenido de los recursos relacionados. Herencia, realización y dependencia se documentan para refinamiento manual.
- Ollama es una integración opcional de la app generada mediante `OLLAMA_BASE_URL` y `OLLAMA_MODEL`; UMLink no se conecta, instala ni depende de Ollama.
- Los ZIP no se persisten en UMLink y el código generado requiere revisión humana antes de producción.
- La validación de referencia incluye compilar un ZIP con Java 21, aplicar Flyway sobre PostgreSQL y consultar sus recursos relacionados. Para Flutter Web, validar además el origen CORS; Windows requiere la carga de trabajo **Desktop development with C++** de Visual Studio.

## Próximo incremento acordado

Habilitar el consumo seguro desde Angular y terminar el flujo vertical de proyectos:

1. [x] CORS para `http://localhost:4200`.
2. [x] Validar `GET /api/projects` y `POST /api/projects` con pruebas de backend.
3. [x] Conectar la pantalla de proyectos del frontend a esas rutas.
4. [x] Implementar la consulta y creación de diagramas desde un proyecto abierto.
5. [x] Cargar un diagrama real en el editor y permitir crear su primera clase UML.
6. [x] Agregar atributos y relaciones tipadas con cardinalidad desde el editor, incluida la actualización manual de cardinalidades.
7. [x] Persistir estilos de clase y etiquetas de relación; restringir tipos de atributo y cardinalidades válidos.
8. [x] Persistir clase de asociación opcional, punto de quiebre de conectores y su validación de pertenencia al diagrama.
9. [x] Crear una clase intermedia desde una operación transaccional de asociación, sin elementos parciales.
10. [x] Persistir hasta veinte puntos de alineación por relación y omitir cardinalidad en clases intermedias.

No iniciar una fase posterior si la fase actual no cumple los estándares de `02_CALIDAD_OBLIGATORIA.md`.
