# Arquitectura obligatoria

El backend usa arquitectura por funcionalidad. Cada funcionalidad mantiene sus capas juntas:

```text
features/
└── platos/
    ├── controller/   # entrada HTTP o WebSocket
    ├── dto/          # solicitudes y respuestas públicas
    ├── model/        # entidades JPA y enumeraciones
    ├── repository/   # acceso a PostgreSQL
    └── service/      # reglas de negocio y transacciones
```

También existen carpetas transversales:

```text
config/               # configuración técnica: WebSocket, seguridad futura
common/exception/     # errores compartidos y su traducción HTTP
features/collaboration/ # eventos y coordinación WebSocket
features/project/     # proyectos de trabajo
```

## Responsabilidades estrictas

- **Controller**: recibe DTO validado, llama un service y devuelve DTO/respuesta HTTP. No usa repositories.
- **Service**: contiene las reglas de negocio, transacciones y logs significativos. No conoce HTTP.
- **Repository**: solo consulta o persiste entidades. No toma decisiones de negocio.
- **Model**: representa datos persistidos. No debe conocer controllers ni requests HTTP.
- **DTO**: define el contrato externo. Las entidades JPA no se exponen directamente.

## Dependencias permitidas

```text
controller → service → repository → model
controller → dto
service → dto, model, repository
```

No invertir esta dirección ni saltar capas.

## Nota sobre `platos`

La carpeta `features/platos` se mantiene por petición explícita de la usuaria. Sus clases internas actuales representan el dominio UML. No cambiar nombres, rutas REST ni esquema de datos por esa diferencia sin una instrucción explícita.
