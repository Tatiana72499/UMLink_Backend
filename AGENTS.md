# AGENTS.md — Contrato de trabajo para IA

Este archivo es la entrada obligatoria para cualquier IA, agente o desarrollador que trabaje en este repositorio. Estas instrucciones tienen prioridad sobre atajos de implementación.

## Misión y prioridad

Construir un backend confiable para una herramienta colaborativa de diagramas UML. El proyecto debe priorizar **CALIDAD, claridad y comportamiento correcto** sobre velocidad, cantidad de funcionalidades o soluciones improvisadas.

La calidad es un requisito funcional: un cambio no está terminado si compila por casualidad, no está probado, rompe capas, expone datos internos, ignora validaciones o deja documentación desactualizada.

## Lectura obligatoria

Antes de modificar código, leer los documentos en `agents/` en este orden:

1. `00_CONTEXTO_PROYECTO.md`
2. `01_ARQUITECTURA.md`
3. `02_CALIDAD_OBLIGATORIA.md`
4. `03_FLUJO_DE_TRABAJO.md`
5. `04_API_Y_DATOS.md`
6. `05_ROADMAP.md`
7. `06_TESTING.md`
8. `07_SEGURIDAD.md`
9. `08_DECISIONES_ADR.md`
10. `09_OBSERVABILIDAD.md`
11. `10_DEFINICION_DE_HECHO.md`

## Estado técnico actual

- Backend: Spring Boot 3.5.4 + Java 21 + Maven.
- Persistencia: PostgreSQL 18 con Flyway y JPA.
- Base local: `uml_collab` en `localhost:5432`.
- Puerto HTTP: `8080`.
- Raíz de arquitectura: `src/main/java/com/examensw1/umlcollab/`.
- Las funcionalidades usan la estructura `controller`, `dto`, `model`, `repository` y `service`.
- El nombre literal requerido del módulo principal es `features/platos/`.
- Ese módulo todavía contiene el modelo de diagramas UML; no convertirlo en dominio de restaurante ni renombrar sus clases sin una orden explícita de la usuaria.

## Reglas no negociables

- Controller → Service → Repository → Model. No saltar capas.
- Nunca exponer entidades JPA directamente: usar DTOs de entrada y respuesta.
- Validar todas las entradas de API, WebSocket, IA y XML.
- Una migración Flyway aplicada no se edita: se agrega una nueva migración.
- No incluir secretos, credenciales, tokens ni contraseñas en código, logs o commits.
- No abrir CORS/WebSocket a cualquier origen sin autorización explícita.
- No realizar operaciones destructivas en PostgreSQL, Git o archivos sin autorización explícita.
- Todo cambio debe dejar el proyecto más claro o igual de claro; no introducir deuda técnica deliberada.

## Ciclo obligatorio de cada tarea

1. Revisar contexto y archivos afectados.
2. Definir el contrato de API y las reglas de negocio.
3. Implementar respetando arquitectura y calidad.
4. Añadir o actualizar pruebas según `06_TESTING.md`.
5. Ejecutar la verificación mínima:

```powershell
mvn test
```

6. Informar qué cambió, cómo se verificó y cualquier limitación pendiente.

## Comandos locales

```powershell
mvn test
mvn spring-boot:run
```

La definición final de “terminado” está en `agents/10_DEFINICION_DE_HECHO.md`.
