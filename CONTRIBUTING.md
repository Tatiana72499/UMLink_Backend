# Guía de contribución

## Antes de modificar

Lee `AGENTS.md` y los documentos de `agents/`. Respeta la arquitectura y la definición de hecho.

## Ramas sugeridas

```text
main
develop
feature/nombre-corto
fix/nombre-corto
```

No trabajar directamente sobre `main`.

## Commits

Usar mensajes claros:

```text
feat(diagram): agrega creación de clase UML
fix(diagram): valida relación entre clases del diagrama
test(diagram): cubre error al crear atributo
docs: actualiza estrategia de pruebas
```

## Revisión

Antes de solicitar revisión:

1. Ejecutar `mvn test`.
2. Revisar que no haya secretos ni archivos locales incluidos.
3. Explicar en el cambio qué se implementó, cómo se verificó y qué limitaciones quedan.
