# Diseño de pruebas — funcionalidad "objects" (`ObjectResourceImpl`)

Documento de diseño y evidencia de las pruebas de `ObjectResourceImpl`
(ver / actualizar / borrar un objeto de una página vía REST), versión release
`18.4.0` (idéntica a la working copy en esta clase).

---

## 1. Tabla de caminos (base del diseño TDD)

Los casos se derivaron **antes de escribir el código de prueba** leyendo los 3
métodos de la clase real (principio *Timely* de FIRST). Ramas de decisión:

| # | Método | Camino | Resultado esperado | Test que lo cubre |
|---|---|---|---|---|
| 1 | getObject | documento OK y objeto existe | objeto REST | `getObject_WhenObjectExists_ShouldReturnRestObject` |
| 2 | getObject | objeto no existe (`getBaseObject == null`) | WAE **404** | `getObject_WhenObjectDoesNotExist_ShouldThrowNotFound` |
| 3 | getObject | página no existe (`doc.isNew()`) | WAE **404** | `getObject_WhenPageDoesNotExist_ShouldThrowNotFound` |
| 4 | getObject | XWikiException del almacenamiento | **XWikiRestException** (causa preservada) | `getObject_WhenXWikiFails_ShouldThrowXWikiRestException` |
| 5 | updateObject | sin permiso EDIT | WAE **401**, sin `save` | `updateObject_WhenUserHasNoEditRight_ShouldThrowUnauthorized` |
| 6 | updateObject | objeto no existe | WAE **404**, sin `save` | `updateObject_WhenObjectDoesNotExist_ShouldThrowNotFound` |
| 7 | updateObject | éxito, `minorRevision=null` | **202** + `toObject` + `save("", false)` | `updateObject_WhenValid_ShouldSaveAndReturnAccepted` |
| 8 | updateObject | éxito, `minorRevision=true` | `save("", true)` | `updateObject_WhenMinorRevision_ShouldSaveAsMinorEdit` |
| 9 | updateObject | `save` lanza XWikiException | **XWikiRestException** | `updateObject_WhenSaveFails_ShouldThrowXWikiRestException` |
| 10 | deleteObject | sin permiso EDIT | WAE **401**, sin `removeObject` | `deleteObject_WhenUserHasNoEditRight_ShouldThrowUnauthorized` |
| 11 | deleteObject | objeto no existe | WAE **404** | `deleteObject_WhenObjectDoesNotExist_ShouldThrowNotFound` |
| 12 | deleteObject | éxito | `removeObject` **antes de** `save` (InOrder) | `deleteObject_WhenValid_ShouldRemoveObjectAndSave` |
| 13 | deleteObject | `save` lanza XWikiException | **XWikiRestException** | `deleteObject_WhenSaveFails_ShouldThrowXWikiRestException` |

**13/13 caminos → 100% de líneas y ramas de la clase** (verificable con JaCoCo
en `correr-objects.ps1 sonar`).

Caso 14 (adoptado del **generador de casos con IA**, ver
`ai/generador-casos/evidencia-ultima-ejecucion.md`):
`getObject_WhenWikiNameIsNull_ShouldThrowIllegalArgumentException` — los
nombres nulos se rechazan con `IllegalArgumentException` antes de tocar el
almacenamiento, y esa excepción NO la envuelve el `catch (XWikiException)`.

---

## 2. Los 5 dobles de prueba (dónde aparece cada uno)

| Tipo | Definición | Dónde se usa |
|---|---|---|
| **Dummy** | se pasa solo para llenar la firma | `withPrettyNames` en `getObject` (no influye en lo evaluado); `restObject` en el camino 401 (nunca se toca) |
| **Fake** | implementación real ligera | `FakeXWikiContextProvider` (Provider en memoria) y `FakeAlmacenDeObjetos` (mapa número→objeto que respalda `XWikiDocument.getObject`) |
| **Stub** | respuestas predefinidas | `when(apiXWiki.getDocument(...)).thenReturn(doc)`, `when(doc.getObject(...)).thenReturn(...)`, `when(authorization.hasAccess(...)).thenReturn(...)`, etc. |
| **Spy** | objeto real observado | `spy(new FakeXWikiContextProvider(...))` → `verify(providerSpy, atLeastOnce()).get()` demuestra que `getBaseObject` consulta el contexto |
| **Mock** | doble programado y verificado | `Document`, `XWiki` (api y core), `XWikiDocument`, `api.Object`, `ModelFactory` y `ContextualAuthorizationManager` (`@MockComponent`), con `verify(...)` e `InOrder` |

## 3. AAA, FIRST y aserciones fluidas

- **AAA**: cada `@Test` está seccionado con `// Arrange`, `// Act`, `// Assert`.
- **FIRST**: *Fast* (todo en memoria), *Isolated* (cada test crea sus dobles),
  *Repeatable* (entradas fijas), *Self-validating* (Hamcrest/JUnit deciden),
  *Timely* (diseñados desde la tabla de caminos, sección 1).
- **Aserciones fluidas**: Hamcrest (`assertThat(x, is(...))`, `sameInstance`,
  `instanceOf`, `greaterThanOrEqualTo`) en unitarias y en los pasos BDD.
- **Verificaciones de interacción**: `verify(...)`, `never()`, `atLeastOnce()`
  e `inOrder` (p. ej. `removeObject` **antes** de `save`).

---

## 4. BDD: Gherkin + Cucumber + Screenplay (`bdd/`)

`bdd/src/test/resources/features/objetos.feature` describe la funcionalidad en
**Gherkin completo en español** (`# language: es`): 9 escenarios con
`Característica / Antecedentes / Dado / Cuando / Entonces / Y` que cubren los
mismos caminos de negocio (consulta, actualización, borrado, permisos, errores).

La implementación de los pasos usa el patrón **Screenplay**:

```
Actor ("el editor")
  └── Habilidad: UsarLaApiDeObjetos  (recurso REAL + wiki simulado con dobles)
  └── Tareas:    ConsultarObjeto, ActualizarObjeto, EliminarObjeto
  └── Preguntas: Respuestas.elCodigoHttpDeError(), EstadoDeLaPagina.siTieneElObjeto(...), ...
```

- El **estado** de la página vive en un almacén fake en memoria, así los pasos
  `Entonces` preguntan por estado observable (estilo BDD), no solo por mocks.
- Cada escenario es independiente (hook `@Before`/`@After` crea y cierra el
  mundo, incluido el `mockStatic`).
- Reporte HTML: `bdd/target/cucumber-objetos.html`.

TDD + BDD: la suite unitaria nace de la tabla de caminos (diseño primero) y la
BDD expresa los mismos comportamientos como contratos de negocio ejecutables;
ambas corren contra la **misma clase real**, ninguna contra copias.

---

## 5. E2E, UX e IA (entorno real)

- **Cypress** (`e2e/`): ciclo de vida completo contra el XWiki de
  `docker-compose` — crear página y objeto, `GET` 200 fiel, `GET` 404,
  `PUT` 202 + verificación del valor, `PUT`/`DELETE` sin credenciales
  (401/403 y el objeto sobrevive), `DELETE` 204 + `GET` 404. UX: login usable,
  el objeto (etiqueta) visible en la página, presupuesto de tiempo de la API.
- **DeepEval** (`ai/deepeval/`): juez LLM (métrica GEval) que evalúa la
  fidelidad REST de la respuesta real y la claridad del 404 (sin stack traces).
- **Stagehand** (`ai/stagehand/`): navegador dirigido por IA que inicia sesión
  y extrae las etiquetas visibles para confirmar que el objeto creado por la
  API se refleja en la interfaz.
- **Generador de casos frontera** (`ai/generador-casos/`): le pide a un LLM
  analizar la clase + la suite y proponer casos faltantes (IA como copiloto de
  diseño de pruebas).

---

## 6. Evidencia de ejecución

```text
mvn test -f tests/vyv-rest/proyecto-modulo-real/pom.xml -Dtest=ObjectResourceImplTest
  -> Tests run: 14, Failures: 0, Errors: 0, Skipped: 0  (BUILD SUCCESS)
mvn test -f tests/vyv-objects/bdd/pom.xml
  -> Tests run: 9, Failures: 0, Errors: 0 (9 escenarios Cucumber en verde, BUILD SUCCESS)
```

Reportes: `target/surefire-reports/` de cada proyecto y
`bdd/target/cucumber-objetos.html`.

Evidencia de la batería completa ejecutada (incluye JaCoCo 100%, SonarQube con
Quality Gate **PASSED** y todo en **A**, E2E Cypress 7/7, UX 3/3, DeepEval 3/3 y
Stagehand OK contra el XWiki real): ver `CHECKLIST-CUMPLIMIENTO.md` y la carpeta
local `evidencias/`.
