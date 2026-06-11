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
| 9 | **Pruebas E2E** | Doble cobertura: `e2e/cypress/e2e/objects-api.cy.js` (ciclo real GET/PUT/DELETE + 404/401) **y** la suite Playwright `playwright/tests/objetos-api.spec.js` | `npx cypress run` y `npx playwright test` con XWiki arriba |
| 10 | **Pruebas de Experiencia de Usuario** | `e2e/cypress/e2e/objects-ux.cy.js` + `cypress/.../ui/objetos.ui.cy.js` + `a11y/objetos.a11y.cy.js`: login usable, objeto reflejado en la página, accesibilidad WCAG 2.1 AA, presupuesto de tiempo | `npm run test:objects:ui` / `:a11y` |
| 11 | **Cypress — 6 categorías** | En el proyecto COMPARTIDO `../vyv-rest/cypress/cypress/e2e/<cat>/objetos.<cat>.cy.js`: **api, security, performance, a11y, ui, regression** (16 tests, todos verdes). Una sola corrida registra objetos + comentarios + etiquetas | `cd ../vyv-rest/cypress && npm run test:objects` (o `npx cypress run` para las 3 funcionalidades) |
| 11b | **Playwright** (2º motor E2E) | `playwright/tests/objetos-{api,ui}.spec.js`: ciclo REST con el `request` fixture + login y verificación en la UI con navegador real (7 tests verdes) | `cd playwright && npx playwright test` |
| 12 | **Katalon** | `e2e/katalon/ObjectsApiKatalon.groovy`: mismo ciclo E2E con keywords WS de Katalon Studio (pegar en un Test Case en modo Script) | Ejecutable desde Katalon Studio |
| 13 | **Jenkins** | `tests/vyv-objects/Jenkinsfile`: unitarias → BDD → worktree 18.4.0 → JaCoCo+Sonar (gate) → deploy Docker → Cypress; configurar job con Script Path `tests/vyv-objects/Jenkinsfile` | Reportes `junit` + artefactos archivados por el pipeline |
| 14 | **Docker** | `docker-compose.yml` (raíz): XWiki + PostgreSQL; usado por E2E, UX, DeepEval y Stagehand; el pipeline Jenkins hace `docker compose pull/up` | `docker compose up -d` → `http://localhost:8080` |
| 15 | **SonarQube — quality gate al full** | `correr-objects.ps1 sonar -token sqp_...`: JaCoCo + análisis con `projectKey=Xwiki-objects` e `inclusions` SOLO de `ObjectResourceImpl.java`. Los 14 tests cubren **100% de líneas y ramas** de la clase y no añaden código de producción (0 bugs/smells nuevos) → gate en verde | Dashboard `http://localhost:9000/dashboard?id=Xwiki-objects` |
| 16 | **IA 1: DeepEval** | `ai/deepeval/`: métricas GEval (LLM-as-judge) sobre las respuestas REALES de la API (fidelidad REST del GET, claridad del 404). Juez: **Groq** (`groq_judge.py`, `GROQ_API_KEY`) u OpenAI | `pytest test_objects_api_deepeval.py -v` con XWiki + key |
| 17 | **IA 2: Stagehand** | `ai/stagehand/objects-stagehand.mjs`: navegador dirigido por LLM (act/extract) que inicia sesión y verifica que el objeto creado por la API se ve en la interfaz. Modelo Groq Llama u OpenAI | `npm run verificar` con XWiki + key |
| 18 | **IA 3 (a criterio): generador de casos frontera** | `ai/generador-casos/genera-casos-frontera.mjs`: el LLM (Groq) analiza la clase real + la suite y propone casos faltantes | **Ejecutado**: ver `ai/generador-casos/evidencia-ultima-ejecucion.md`; su propuesta se adoptó como test 14 de la suite |
| 19 | **Playwright (herramienta de testing IA adicional)** | `playwright/` con `@playwright/test`: navegador real headless dirigido por código, API + UI | **Ejecutado 7/7** — `evidencias/14-playwright-objetos.txt` |
| 20 | **Registro unificado del equipo** | `../vyv-rest/correr-todo.ps1`: un comando corre y registra unitarias (45) + BDD etiquetas/comentarios + BDD objetos + Cypress compartido (3 funcionalidades) + Playwright (+IA con `-ia`) | **Ejecutado** — `evidencias/16-orquestador-correr-todo.txt` |

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
E2E Cypress (e2e/)  : api 7/7 + ux 3/3 passed contra XWiki real en Docker
Cypress compartido  : objetos 16/16 (api 4, security 3, performance 3, a11y 2, ui 2, regression 2);
                      comentarios 20/20; (etiquetas ui/a11y necesitan el flavor — specs del compañero)
Playwright objetos  : 7/7 passed (api 5 + ui 2), segundo motor de navegador
IA DeepEval (Groq)  : 3/3 passed contra la API real (juez LLM)
IA Stagehand (Groq) : OK — la IA inició sesión y verificó el objeto en el editor de objetos
IA generador        : ejecutado; su propuesta se adoptó como test 14 de la suite
Orquestador         : correr-todo.ps1 -e2e — unitarias OK, BDD x2 OK, Playwright OK,
                      Cypress compartido registra las 3 funcionalidades en una corrida
```

Salidas completas, capturas y reportes en `evidencias/` (carpeta local, excluida de git).
Evidencias nuevas: `12`/`13` (Cypress objetos 6 categorías), `14` (Playwright),
`15` (Cypress compartido completo, 3 funcionalidades), `16` (orquestador consolidado).

**Sobre los 2 rojos del run compartido**: son `ui/etiquetas` y `a11y/etiquetas`
(funcionalidad *page tags*, de otro compañero). Dependen del panel de etiquetas
`#xdocTags` que aporta el **flavor** de XWiki, no instalado en este entorno local.
No se modificaron (instrucción: no tocar otras funcionalidades). Las 6 categorías
de **objetos** pasan al 100%.

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
