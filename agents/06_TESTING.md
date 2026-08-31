# Estrategia de pruebas

## Objetivo

Las pruebas protegen el comportamiento del proyecto. No son un trámite: deben detectar regresiones antes de que lleguen a la demostración o a otro integrante del equipo.

## Pirámide de pruebas

1. **Unitarias**: services y validadores. No levantan Spring ni PostgreSQL.
2. **Integración**: repositories, Flyway y servicios con PostgreSQL de prueba.
3. **Web/API**: controllers con MockMvc; validan estado HTTP, DTO y manejo de errores.
4. **Flujo crítico**: Proyecto → Diagrama → Clase → Atributo/Relación → consulta del diagrama.

## Mínimos por funcionalidad

- Caso exitoso.
- Dato inválido o incompleto.
- Recurso inexistente (`404`).
- Regla de negocio incumplida (`400` o `409`, según corresponda).
- Verificación de que no se persisten cambios parciales tras un error.

## Convenciones

- Nombre: `debeCrearProyectoCuandoSolicitudEsValida`.
- Patrón: preparar, ejecutar, verificar.
- Una prueba debe verificar un comportamiento concreto.
- No depender del orden de ejecución de otras pruebas.
- Usar datos de prueba explícitos y legibles.

## Puerta de calidad

Antes de integrar cambios se ejecuta:

```powershell
mvn test
```

Cuando se añada una función importante, su prueba debe añadirse en el mismo cambio. No se acepta el argumento “la agregaremos después”.
