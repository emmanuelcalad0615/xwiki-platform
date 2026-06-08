# Pruebas unitarias automatizadas — Comentarios (JUnit 5 + Mockito)

Código fuente de las pruebas unitarias sobre la parte seleccionada del software
(módulo REST de comentarios), con patrón **AAA**, principios **FIRST** y los
**5 tipos de dobles de prueba (mocks)**.

| Archivo | Clase bajo prueba | Tests | Caminos |
|---|---|---|---|
| `codigo-fuente/CommentsResourceImplTest.java` | `CommentsResourceImpl` (listar + crear) | 9 | 1-9 |
| `codigo-fuente/CommentResourceImplTest.java` | `CommentResourceImpl` (obtener uno) | 4 | 10-13 |
| `codigo-fuente/CommentsVersionResourceImplTest.java` | `CommentsVersionResourceImpl` (historial lista) | 3 | 14-16 |
| `codigo-fuente/CommentVersionResourceImplTest.java` | `CommentVersionResourceImpl` (historial uno) | 3 | — |

**Total: 19 pruebas** cubriendo los 16 caminos del módulo REST de comentarios.

---

## Los 5 tipos de dobles de prueba (dónde aparece cada uno)

| # | Tipo | Definición | Dónde se usa en el código |
|---|------|-----------|---------------------------|
| 1 | **Dummy** | Objeto que se pasa solo para llenar la firma; nunca se usa en el camino. | Parámetro `withPrettyNames` (Boolean) en `getComments_*` — se pasa `false` pero no influye en el resultado evaluado. |
| 2 | **Fake** | Implementación real pero ligera/simplificada. | Clase `FakeCommentRepository` (almacén de comentarios en memoria) que respalda `doc.getComments()`. |
| 3 | **Stub** | Devuelve respuestas predefinidas. | `when(apiDoc.getComments()).thenReturn(...)`, `when(doc.createNewObject(...)).thenReturn(0)`, etc. |
| 4 | **Spy** | Objeto real al que se le verifican/observan interacciones. | `spy(new ObjectFactory())` inyectado en el recurso; se verifica `createComments()`. |
| 5 | **Mock** | Doble completo programado y verificado. | `mock(Document.class)`, `mock(XWiki.class)`, `mock(api.Object.class)` con `verify(...)`. |

---

## AAA y FIRST

- **AAA**: cada `@Test` está dividido con comentarios `// Arrange`, `// Act`, `// Assert`.
- **FIRST**:
  - *Fast*: sin I/O real (todo en memoria, con dobles).
  - *Isolated*: cada test crea sus propios dobles; no comparten estado.
  - *Repeatable*: entradas fijas → mismo resultado siempre.
  - *Self-validating*: aserciones JUnit (pasa/falla automático).
  - *Timely*: derivadas del diseño (tabla de caminos) antes de la evidencia.

---

## Caminos cubiertos por las automatizadas

Incluyen los caminos de **excepción** que las manuales no pueden forzar:

| Test | Camino backend | Tipo |
|---|---|---|
| `getComments_WhenPageHasNoComments_ShouldReturnEmptyList` | 2 | ruta normal |
| `getComments_WhenPageHasComments_ShouldReturnThem` | 3 (bucle) | ruta normal |
| `getComments_WhenDocumentFails_ShouldThrowXWikiRestException` | 1 (excepción 500) | **fault injection** |
| `postComment_WhenBodyHasText_ShouldCreateAndReturn201` | 7 | ruta normal |
| `postComment_WhenBodyHasNoContent_ShouldReturnNullWithoutSaving` | 9 (return null) | borde |
| `postComment_WhenUserHasNoRights_ShouldThrowUnauthorized` | 4 (401) | error |
| `getComment_WhenIdExists_ShouldReturnComment` | 12 | ruta normal |
| `getComment_WhenIdNotFound_ShouldThrowNotFound` | 11 (404) | error |
| `getComment_WhenDocumentFails_ShouldThrowXWikiRestException` | 10 (excepción 500) | **fault injection** |

---

## Cómo ejecutarlas (evidencia de ejecución)

Las pruebas usan el framework de test de XWiki (`@ComponentTest`), por lo que se
ejecutan dentro del módulo con Maven:

1. Copiar los `.java` a la ruta de tests del módulo:
   ```
   xwiki-platform-core/xwiki-platform-rest/xwiki-platform-rest-server/
     src/test/java/org/xwiki/rest/internal/resources/comments/
   ```
2. Ejecutar solo estas pruebas:
   ```bash
   mvn test -pl :xwiki-platform-rest-server -am -Dtest=CommentsResourceImplTest,CommentResourceImplTest
   ```
3. La salida `BUILD SUCCESS` + el reporte en `target/surefire-reports/` es la
   evidencia de ejecución.

> Dependencias ya disponibles en el módulo: JUnit 5, Mockito (incluye
> `mockStatic`), `xwiki-platform-test-junit5`. No requieren añadir nada.
