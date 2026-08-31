# Seguridad

## Principios

- Mínimo privilegio: cada usuario solo puede leer o modificar los proyectos donde es miembro.
- Validar toda entrada HTTP, WebSocket, XML y comandos de IA.
- No confiar en identificadores enviados por el cliente sin comprobar su pertenencia.
- Devolver errores seguros: nunca exponer trazas, consultas SQL, contraseñas o tokens.

## Secretos y configuración

- Nunca subir `.env`, contraseñas ni claves JWT al repositorio.
- Usar variables de entorno para credenciales y secretos.
- Mantener un `.env.example` sin valores reales cuando se incorpore autenticación.

## Autenticación futura

- JWT firmado con una clave externa al código.
- Contraseñas almacenadas únicamente con hash robusto (BCrypt o Argon2).
- Expiración de tokens y renovación definida antes de liberar la función.
- Roles iniciales: `OWNER`, `EDITOR`, `VIEWER`.

## WebSocket y CORS

- No aceptar todos los orígenes con `*`.
- Permitir únicamente los orígenes concretos de los frontends conocidos.
- Asociar cada evento WebSocket a un usuario autenticado y validar que pertenece al diagrama.

## IA, XML e importación

- La IA produce comandos estructurados; el service los valida antes de ejecutarlos.
- Proteger los importadores XML contra entidades externas (XXE) y archivos excesivamente grandes.
- No permitir que un comando de IA ejecute código, SQL o rutas de archivos arbitrarias.
