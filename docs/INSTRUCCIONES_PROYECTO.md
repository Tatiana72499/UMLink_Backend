# Instrucciones del proyecto

## Herramienta colaborativa para diseño de datos

- El proyecto se centra en una herramienta de diseño de datos basada principalmente en diagramas de clases.
- Debe permitir trabajo colaborativo, como una pizarra virtual compartida, para que varias personas intervengan sobre el mismo modelo.
- Las herramientas existentes, como ArchiTect, no cubren el requisito porque no ofrecen la colaboración requerida.
- El sistema debe controlar exclusión y coordinación cuando varios usuarios editan simultáneamente un mismo diseño.

## IA y edición asistida

- La herramienta tendrá una capa de IA para asistir al diseñador.
- La IA no debe generar el diagrama completo desde cero; ejecutará operaciones concretas:
  - crear clases;
  - eliminar elementos;
  - agregar atributos;
  - mover objetos;
  - establecer relaciones.
- La interacción puede realizarse mediante comandos de voz, sin depender exclusivamente de teclado y mouse.
- La IA aumenta la productividad del diseñador, pero no lo reemplaza.

## Generación de backend e integración

- La herramienta debe generar código backend a partir de un diagrama de clases.
- El backend generado debe usar capas típicas y PostgreSQL como base de datos.
- Debe permitir importar y exportar modelos mediante un formato XML especializado o estándar compatible.
- La integración debe permitir continuar modelos iniciados en otras herramientas.

## Aplicación móvil, modo sin conexión y validación

- La presentación final incluirá una aplicación móvil que consuma el backend generado.
- El frontend móvil debe admitir interacción por voz, en especial cuando no exista una interfaz tradicional.
- Debe funcionar sin conexión: los cambios se guardan localmente y se sincronizan al recuperar internet.
- La guía local de IA debe vivir en el teléfono para que la asistencia básica funcione sin conectividad.
- En la presentación se evaluarán la herramienta colaborativa, la generación de código y sus funciones clave. El frontend móvil se desarrollará en clase con Flutter o React.

## Decisiones iniciales de implementación

- Backend de la herramienta: Spring Boot + PostgreSQL + WebSocket.
- Colaboración: servidor autoritativo, eventos de edición y control optimista de versión.
- Fuente única de verdad: el modelo UML persistido (diagramas, clases, atributos y relaciones).
- La IA, XML y el generador de backend deben operar sobre acciones y el mismo modelo UML.
- Alcance inicial: consolidar CRUD de diagramas y colaboración antes de abordar IA, generación, móvil u operación offline.
