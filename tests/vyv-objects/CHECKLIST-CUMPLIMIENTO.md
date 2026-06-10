# Checklist de cumplimiento — V&V de la funcionalidad "objects" (`ObjectResourceImpl`)

Mapa requisito → artefacto → evidencia. Todas las rutas son relativas a la raíz
del repo. La clase bajo prueba es la REAL de XWiki (release 18.4.0):
`org.xwiki.rest.internal.resources.objects.ObjectResourceImpl`
(getObject / updateObject / deleteObject).

| # | Requisito | Dónde está implementado | Evidencia de ejecución |
|---|---|---|---|
| 1 | **TDD** | Suite diseñada desde la **tabla de caminos** antes de codificar (ver `PRUEBAS-OBJECTS.md` §1) y ciclo cerrado con IA: el caso 14 (`getObject_WhenWikiNameIsNull_...`) nació de la propuesta del generador LLM | `mvn test -f tests/vyv-rest/proyecto-modulo-real/pom.xml -Dtest=ObjectResourceImplTest` → 14/14 verde |
| 2 | **BDD** | `tests/vyv-objects/bdd/` (Cucumber JVM) | `mvn test -f tests/vyv-objects/bdd/pom.xml` → 9 escenarios verdes |
| 3 | **Gherkin completo (español)** | `bdd/src/test/resources/features/objetos.feature`: `Característica`, `Antecedentes`, `Dado/Cuando/Entonces/Y`, 9 escenarios | Reporte `bdd/target/cucumber-objetos.html` |
| 4 | **Screenplay** | `bdd/src/test/java/com/vyv/objects/bdd/screenplay/`: `Actor`, `Tarea`, `Pregunta` + habilidad `UsarLaApiDeObjetos` + tareas `Consultar/Actualizar/EliminarObjeto` + preguntas `Respuestas`/`EstadoDeLaPagina` | Mismo run BDD (los pasos delegan en el actor "el editor") |
| 5 | **Test doubles — los 5 tipos** | `ObjectResourceImplTest.java`: **Dummy** (`withPrettyNames`, `restObject` en 401), **Fake** (`FakeXWikiContextProvider`, `FakeAlmacenDeObjetos`), **Stub** (todos los `when(...).thenReturn`), **Spy** (`spy(new FakeXWikiContextProvider(...))` + `verify(spy, atLeastOnce()).get()`), **Mock** (`Document`, `XWiki` api/core, `XWikiDocument`, `api.Object`, `@MockComponent ModelFactory` y `ContextualAuthorizationManager` con `verify`/`InOrder`) | Cada doble está etiquetado con comentario `// MOCK`, `// STUB`, `// SPY`, `// FAKE`, `// DUMMY` en el código |
| 6 | **Patrón AAA** | Cada `@Test` seccionado `// Arrange`, `// Act`, `// Assert`; también en los pasos BDD (Given=Arrange, When=Act, Then=Assert) | Revisión del código + runs verdes |
| 7 | **Principios FIRST** | Fast (todo en memoria, suite < 3 s), Isolated (dobles propios por test), Repeatable (entradas fijas), Self-validating (Hamcrest/JUnit), Timely (tabla de caminos primero) — detalle en `PRUEBAS-OBJECTS.md` §3 | Tiempos en `target/surefire-reports/` |
| 8 | **Fluent Assertions** | Hamcrest en unitarias y BDD: `assertThat(x, is(...))`, `sameInstance`, `instanceOf`, `notNullValue`, `greaterThanOrEqualTo`; encadenadas con matchers compuestos `is(instanceOf(...))` | Mismos runs |
| 9 | **Pruebas E2E** | `e2e/cypress/e2e/objects-api.cy.js`: ciclo completo real (crear página → crear objeto → GET 200 → 404 → PUT 202 → 401/403 → DELETE 204 → GET 404) contra el XWiki de docker-compose | `npx cypress run` con XWiki arriba |
| 10 | **Pruebas de Experiencia de Usuario** | `e2e/cypress/e2e/objects-ux.cy.js`: login usable (campos visibles/etiquetados), el objeto se refleja como etiqueta visible en la página, presupuesto de tiempo de respuesta de la API (<3 s) | `npx cypress run --spec cypress/e2e/objects-ux.cy.js` |
| 11 | **Cypress** | Proyecto `e2e/` (package.json + cypress.config.js + 2 specs) | ídem #9/#10 |
| 12 | **Katalon** | `e2e/katalon/ObjectsApiKatalon.groovy`: mismo ciclo E2E con keywords WS de Katalon Studio (pegar en un Test Case en modo Script) | Ejecutable desde Katalon Studio |
| 13 | **Jenkins** | `tests/vyv-objects/Jenkinsfile`: unitarias → BDD → worktree 18.4.0 → JaCoCo+Sonar (gate) → deploy Docker → Cypress; configurar job con Script Path `tests/vyv-objects/Jenkinsfile` | Reportes `junit` + artefactos archivados por el pipeline |
| 14 | **Docker** | `docker-compose.yml` (raíz): XWiki + PostgreSQL; usado por E2E, UX, DeepEval y Stagehand; el pipeline Jenkins hace `docker compose pull/up` | `docker compose up -d` → `http://localhost:8080` |
| 15 | **SonarQube — quality gate al full** | `correr-objects.ps1 sonar -token sqp_...`: JaCoCo + análisis con `projectKey=Xwiki-objects` e `inclusions` SOLO de `ObjectResourceImpl.java`. Los 14 tests cubren **100% de líneas y ramas** de la clase y no añaden código de producción (0 bugs/smells nuevos) → gate en verde | Dashboard `http://localhost:9000/dashboard?id=Xwiki-objects` |
| 16 | **IA 1: DeepEval** | `ai/deepeval/`: métricas GEval (LLM-as-judge) sobre las respuestas REALES de la API (fidelidad REST del GET, claridad del 404). Juez: **Groq** (`groq_judge.py`, `GROQ_API_KEY`) u OpenAI | `pytest test_objects_api_deepeval.py -v` con XWiki + key |
| 17 | **IA 2: Stagehand** | `ai/stagehand/objects-stagehand.mjs`: navegador dirigido por LLM (act/extract) que inicia sesión y verifica que el objeto creado por la API se ve en la interfaz. Modelo Groq Llama u OpenAI | `npm run verificar` con XWiki + key |
| 18 | **IA 3 (a criterio): generador de casos frontera** | `ai/generador-casos/genera-casos-frontera.mjs`: el LLM (Groq) analiza la clase real + la suite y propone casos faltantes | **Ejecutado**: ver `ai/generador-casos/evidencia-ultima-ejecucion.md`; su propuesta se adoptó como test 14 de la suite |

## Las API keys (importante)

Las 3 herramientas de IA usan **una sola key gratuita de Groq** leída de la
variable de entorno `GROQ_API_KEY` (u `OPENAI_API_KEY` si se prefiere):

```powershell
$env:GROQ_API_KEY = "gsk_TU_KEY"     # solo en la sesión; NUNCA commitearla
```

La key **no debe escribirse en ningún archivo del repo** (quedaría pública en
GitHub y cualquiera podría gastarla; además los proveedores las revocan al
detectarlas). Si una key se llegó a exponer en un chat o pantalla compartida,
revócala en la consola de Groq y genera otra.

## Resumen de evidencia — TODA la batería EJECUTADA (2026-06-10)

```text
Unitarias objects   : Tests run: 14, Failures: 0  (módulo real en worktree y mini-pom)
Suite del equipo    : Tests run: 45, Failures: 0, Skipped: 1 (plantilla attachments ajena)
BDD Cucumber        : 9 escenarios, 0 fallos
JaCoCo (clase real) : instrucciones 177/177, ramas 10/10, líneas 36/36, métodos 4/4 = 100%
SonarQube           : Quality Gate PASSED — Security A, Reliability A, Maintainability A,
                      Coverage 100%, 0 bugs, 0 vulnerabilidades, 0 code smells, 0 hotspots
E2E Cypress (API)   : 7/7 passed contra XWiki real en Docker
UX Cypress          : 3/3 passed (login usable, objeto reflejado en la página, presupuesto de tiempo)
IA DeepEval (Groq)  : 3/3 passed contra la API real (juez LLM)
IA Stagehand (Groq) : OK — la IA inició sesión y verificó el objeto en el editor de objetos
IA generador        : ejecutado; su propuesta se adoptó como test 14 de la suite
```

Salidas completas, capturas y reportes en `evidencias/` (carpeta local, excluida de git).

**Hallazgo de la IA (documentado)**: con un criterio estricto, el juez LLM detectó
que la página 404 por defecto del contenedor expone el banner de Apache Tomcat
(`evidencias/10b-hallazgo-ia-404-tomcat.txt`). `ObjectResourceImpl` solo controla
el código de estado; recomendación para producción: configurar páginas de error
personalizadas en el servlet container.

## Nota del entorno E2E local

El XWiki de docker-compose quedó configurado **sin el flavor estándar** (el
asistente de instalación se automatizó con Playwright — capturas en
`evidencias/wizard-instalacion-xwiki/` — pero los timeouts intermitentes de
nexus.xwiki.org impidieron completar la descarga de extensiones). En su lugar se
configuró por REST: usuario `Admin/admin` + grupo de administradores + permisos
(invitado sin edit/delete), que es todo lo que la funcionalidad objects necesita.
Las pruebas E2E/UX/IA corren idénticas con o sin flavor; las credenciales del
entorno local están en `evidencias/CREDENCIALES-LOCALES.txt`.
