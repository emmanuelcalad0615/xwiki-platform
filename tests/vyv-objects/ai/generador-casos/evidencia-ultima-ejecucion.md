# Evidencia de ejecución — generador de casos frontera (LLM vía Groq)

Comando (la key va SOLO en variable de entorno, nunca en el repo):

```powershell
$env:GROQ_API_KEY = "gsk_***"
node genera-casos-frontera.mjs
```

Modelo: `llama-3.3-70b-versatile` (Groq, API compatible con OpenAI).

## Salida obtenida (resumen)

La IA analizó la clase real `ObjectResourceImpl` (18.4.0) junto con la suite
`ObjectResourceImplTest` y propuso 10 casos frontera, todos variantes de
**parámetros nulos/vacíos/negativos** (`wikiName`, `spaceName`, `className`,
`objectNumber`), más la recomendación de **pruebas de mutación** (Pitest) si la
cobertura ya es completa.

## Acción tomada (cierre del ciclo TDD asistido por IA)

De las propuestas, la de mayor valor real es la de **nombres nulos**: en el
código real `getDocumentInfo` rechaza `wikiName == null` con
`IllegalArgumentException` *antes* de tocar el almacenamiento, y esa excepción
NO es envuelta por el `catch (XWikiException)` del recurso. Se adoptó como el
test 14 de la suite:

```
getObject_WhenWikiNameIsNull_ShouldThrowIllegalArgumentException
```

Las variantes de número negativo no se adoptaron: el contrato del método las
trata como "objeto inexistente" (404), camino ya cubierto por
`getObject_WhenObjectDoesNotExist_ShouldThrowNotFound`.
