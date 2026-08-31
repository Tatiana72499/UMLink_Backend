# Observabilidad e incidentes

## Logs

- Registrar en nivel `INFO` acciones relevantes de negocio: crear proyecto, diagrama, clase o relación.
- Usar `WARN` para validaciones relevantes, conflictos y operaciones rechazadas.
- Usar `ERROR` solo para fallos inesperados con contexto técnico útil.
- Nunca registrar contraseñas, JWT, contenido privado, secretos de entorno ni datos innecesarios.

## Contexto mínimo

Cuando esté disponible, los logs deben identificar:

- identificador del proyecto o diagrama;
- identificador de usuario;
- tipo de operación;
- resultado o causa de rechazo.

## Manejo de incidentes

1. Reproducir el problema con pasos concretos.
2. Revisar logs y respuesta HTTP/WebSocket.
3. Crear una prueba que falle por el problema.
4. Corregir la causa, no solo el síntoma.
5. Ejecutar todas las pruebas.
6. Documentar la decisión si cambia la arquitectura o comportamiento público.

## Futuro

Agregar Spring Boot Actuator y health checks antes de desplegar fuera de local.
