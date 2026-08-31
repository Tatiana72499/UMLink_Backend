# Definición de hecho

Una tarea solo se considera **terminada** cuando cumple todos los puntos aplicables:

- El requisito y los criterios de aceptación están claros.
- Respeta la arquitectura por funcionalidad y capas.
- Controller, service, repository, model y DTO tienen responsabilidades correctas.
- Las entradas se validan y los errores se responden de forma consistente.
- La lógica de negocio tiene pruebas nuevas o actualizadas.
- `mvn test` termina exitosamente.
- Si cambió el esquema, existe una nueva migración Flyway probada.
- No incluye secretos, código muerto, duplicación evidente ni configuraciones inseguras.
- La documentación de API, arquitectura o ADR fue actualizada si corresponde.
- La funcionalidad fue probada en el flujo real cuando el cambio afecta integración.

Si uno de estos puntos no se cumple, el estado correcto es “en progreso”, no “terminado”.
