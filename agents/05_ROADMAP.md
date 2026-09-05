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
- [x] Persistir y difundir trazos de lápiz por diagrama para los miembros con permiso de edición.
- [x] Previsualizar trazos, resaltar clases arrastradas y comunicar actividad remota contextual mediante eventos WebSocket efímeros autorizados.

## Autenticación — base para pruebas locales

- [x] Registro e inicio de sesión con contraseñas BCrypt y JWT externo.
- [x] Asociar el creador autenticado a cada proyecto y restringir su acceso al propietario.

## Fase 3 — Interoperabilidad e IA

- [ ] Importar/exportar un subconjunto documentado de XML UML.
- [ ] Compartir un archivo XMI UML interoperable, validado inicialmente contra Enterprise Architect u otro editor compatible con la versión XMI acordada.
- [ ] Definir comandos estructurados: crear clase, atributo, relación, eliminar y mover.
- [ ] Validar y ejecutar comandos IA mediante services existentes.

## Fase 4 — Generación y móvil

- [ ] Generar backend Spring Boot en capas desde el modelo UML.
- [ ] Crear migraciones PostgreSQL del backend generado.
- [ ] Crear app móvil con lectura/escritura y sincronización offline.

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
