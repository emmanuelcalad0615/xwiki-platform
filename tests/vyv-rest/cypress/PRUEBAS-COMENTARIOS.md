# Pruebas E2E de Comentarios (Cypress) — Guía para sustentar

Pruebas de la funcionalidad **Comentarios** (API REST `CommentsResourceImpl` /
`CommentResourceImpl` de XWiki) ejecutadas con **Cypress** contra el XWiki
desplegado en `http://localhost:8080`.

Organización por **tipo de prueba** (carpeta) → `cypress/e2e/<tipo>/comentarios.<tipo>.cy.js`.

Endpoint base: `/rest/wikis/xwiki/spaces/Main/pages/WebHome/comments`

Cómo correr todo: `npm run test:comments`

---

## 1. API — `api/comentarios.api.cy.js`
Caminos felices del recurso REST de comentarios.

| Prueba | Qué hace | Resultado | Unit test que espeja |
|---|---|---|---|
| Obtiene los comentarios (GET 200) | `GET /comments` y valida que la respuesta tiene la lista `comments` | **200** | `getComments_WhenPageHasComments` |
| Crea un comentario (POST 201) | `POST /comments` con `{text}` autenticado; luego `GET` y confirma que el texto quedó guardado | **201** + persiste | `postComment_WhenBodyHasText` |
| Obtiene un comentario por id (GET 200) | Crea uno, lee su `id` de la lista y hace `GET /comments/{id}` | **200** | `getComment_WhenIdExistsFirst` |
| Crea comentario con highlight (POST 201) | `POST` con `{highlight, text}` (comentario sobre un fragmento resaltado) | **201** | `postComment_WhenOnlyHighlight` |

## 2. Seguridad — `security/comentarios.security.cy.js`
Control de acceso: escribir requiere autenticación válida.

| Prueba | Qué hace | Resultado | Unit test |
|---|---|---|---|
| 401 sin autenticación | `POST /comments` **sin** credenciales | **401** | `postComment_WhenUserHasNoRights` |
| 401/403 con credenciales inválidas | `POST` con usuario/clave falsos | **401 o 403** | (mismo camino de autorización) |

## 3. Regresión / casos borde — `regression/comentarios.regression.cy.js`

| Prueba | Qué hace | Resultado | Unit test |
|---|---|---|---|
| 404 id inexistente | `GET /comments/999999` | **404** | `getComment_WhenIdNotFound` |
| Respuesta a comentario (replyTo) | Crea padre y luego una respuesta con `replyTo` | **201** | `postComment_WhenTextAndReplyTo` |
| Comentario con XSS / caracteres especiales | `POST` con `<script>...</script>`; confirma que la API lo acepta y se persiste como texto | **201** | (seguridad de entrada) |
| Comentario vacío | `POST` sin `text` ni `highlight` | **204** (XWiki desplegado lo acepta) | `postComment_WhenBodyHasNoContent` (corregido a **400** en el código) |
| Paginación (start/number) | `GET /comments?start=0&number=1` | **200**, ≤1 elemento | uso de `RangeIterable` |
| El conteo aumenta tras crear | Cuenta antes, crea uno, cuenta después | mayor que antes | (CRUD) |

> **Hallazgo V&V**: el binario desplegado de XWiki acepta un comentario vacío
> (responde 204 sin guardar). El defecto se corrigió en `CommentsResourceImpl`
> para que devuelva **400 Bad Request**, verificado por la prueba unitaria
> `postComment_WhenBodyHasNoContent_ShouldThrowBadRequest`.

## 4. UI Web — `ui/comentarios.ui.cy.js`
Verificación en el navegador (lado lectura). Login programático antes de cada prueba.

| Prueba | Qué hace | Resultado |
|---|---|---|
| Muestra un comentario en la UI | Crea por API y verifica que el texto aparece en `?viewer=comments` | el comentario es visible |
| XSS seguro en la UI | Crea un comentario con `<script>`; verifica que se muestra como **texto** y que **no** existe un `<script>` vivo con el payload | escapado, no se ejecuta |

## 5. Accesibilidad — `a11y/comentarios.a11y.cy.js`
Auditoría WCAG con **cypress-axe** sobre la página de comentarios (logueado).

| Prueba | Qué hace | Resultado |
|---|---|---|
| Sin violaciones WCAG 2.1 AA | `cy.injectAxe()` + `cy.checkA11y()` (excluye `color-contrast`) | 0 violaciones |
| Heading principal (h1) | La página tiene un `h1` no vacío | existe |
| Sección accesible | Usa lista semántica (`ul/ol`) o roles ARIA de lista | presente |

## 6. Performance — `performance/comentarios.performance.cy.js`
Tiempo de respuesta de la API (`res.duration` en ms). Umbral: **2000 ms**.

| Prueba | Qué hace | Resultado |
|---|---|---|
| GET bajo el umbral | mide el tiempo de `GET /comments` | < 2000 ms |
| POST bajo el umbral | mide el tiempo de crear un comentario | < 2000 ms |
| Promedio de 10 GET | hace 10 GET y promedia los tiempos | promedio < 2000 ms |

---

## Mapa rápido para el profe
- **Caja negra / E2E**: todas estas pruebas (entradas/salidas, sin ver el código).
- **Seguridad**: 401/403 + XSS escapado.
- **Regresión**: suite `regression` + el caso de comentario vacío (defecto detectado).
- **Accesibilidad**: `a11y` con cypress-axe (WCAG 2.1 AA).
- **Rendimiento**: `performance` (umbral de tiempo de respuesta).
- **Trazabilidad**: cada caso de API/regresión **espeja** una prueba unitaria
  (Mockito) de la misma funcionalidad → mismo camino probado en 2 niveles.
