# Calidad obligatoria

La calidad es una condición de aceptación para cada cambio. Una funcionalidad no está terminada hasta que cumpla estas reglas.

## Código

- Código claro, pequeño y con una responsabilidad por clase/método.
- Nombres descriptivos y consistentes en inglés para código y paquetes.
- Sin lógica de negocio en controllers ni consultas de base de datos fuera de repositories.
- Sin `catch` vacío, valores mágicos, duplicación innecesaria ni código comentado muerto.
- Validar todos los DTO de entrada con Bean Validation.
- Usar DTOs de respuesta; nunca devolver entidades JPA directamente.
- Registrar en services los eventos relevantes y errores esperados sin escribir secretos en logs.

## Base de datos

- Todo cambio de esquema debe ser una migración nueva de Flyway en `src/main/resources/db/migration/`.
- Nunca editar una migración que ya fue aplicada a una base compartida.
- Mantener nombres de columnas explícitos cuando difieran del campo Java.
- Usar claves foráneas, restricciones e índices cuando correspondan.

## Seguridad y robustez

- No incluir contraseñas, tokens o claves en el repositorio.
- Configurar secretos mediante variables de entorno.
- No usar CORS/WebSocket abierto a cualquier origen en cambios futuros.
- Validar pertenencia y permisos antes de modificar recursos cuando se agregue autenticación.
- Los conflictos de versión deben devolver una respuesta controlada, no un error 500.

## Verificación mínima

Antes de entregar un cambio:

```powershell
mvn test
```

Si una tarea modifica API, añadir o actualizar pruebas. Si modifica base de datos, validar que Flyway aplique correctamente la migración.
