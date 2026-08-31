# Contexto del proyecto

## Propósito

El objetivo académico es una herramienta colaborativa para diseño de datos mediante diagramas de clases UML. Varias personas deben poder trabajar sobre el mismo modelo, recibir cambios en tiempo real y gestionar conflictos de edición.

## Alcance final esperado

- Editor colaborativo de diagramas de clases.
- Operaciones sobre clases, atributos, relaciones y posiciones.
- Asistencia por IA mediante operaciones pequeñas y validadas; no generación autónoma de diagramas completos.
- Comandos de texto y, más adelante, voz.
- Importación y exportación de modelos XML.
- Generación de un backend en capas con PostgreSQL a partir de un diagrama.
- Aplicación móvil que consuma el backend generado, funcione offline y sincronice al reconectarse.

## Prioridad actual

Solo backend. Consolidar persistencia, API REST, validaciones y colaboración WebSocket antes de IA, XML, generador o aplicación móvil.

## Decisiones ya tomadas

- Java 21, Spring Boot, PostgreSQL, Flyway, JPA y WebSocket/STOMP.
- El servidor es autoritativo: valida operaciones, persiste los cambios y los difunde.
- El modelo UML en PostgreSQL es la fuente única de verdad.
- `@Version` se usa como base para detectar ediciones concurrentes.
