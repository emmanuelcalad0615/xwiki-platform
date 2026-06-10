# V&V de la funcionalidad "objects" — `ObjectResourceImpl`

Pruebas de la funcionalidad **objects** de la API REST de XWiki
(`xwiki-platform-core/xwiki-platform-rest/xwiki-platform-rest-server/.../resources/objects/ObjectResourceImpl.java`):
**ver (`getObject`), actualizar (`updateObject`) y borrar (`deleteObject`)** un
objeto de una página. Todo prueba la **clase REAL** del release publicado
`18.4.0` (mismo método del equipo, ver `tests/vyv-rest/CONTEXTO.md`).

## Mapa de artefactos

| Qué | Dónde | Corre con |
|---|---|---|
| **Unitarias** (TDD, AAA, FIRST, 5 dobles, Hamcrest) | `../vyv-rest/proyecto-modulo-real/.../objects/ObjectResourceImplTest.java` | `mvn test -f ../vyv-rest/proyecto-modulo-real/pom.xml -Dtest=ObjectResourceImplTest` |
| **BDD** Gherkin (es) + Cucumber + Screenplay | `bdd/` | `mvn test -f bdd/pom.xml` |
| **E2E + UX** (Cypress, API y UI reales) | `e2e/` | `npm install && npx cypress run` (XWiki arriba) |
| **Katalon** (mismo ciclo E2E, keywords WS) | `e2e/katalon/ObjectsApiKatalon.groovy` | pegar en un Test Case de Katalon Studio (modo Script) |
| **SonarQube** (cobertura solo de la clase) | `correr-objects.ps1 sonar` | worktree `xwiki-184` + Sonar local |
| **Jenkins** (pipeline completo de objects) | `Jenkinsfile` | Job "Pipeline script from SCM" |
| **IA**: DeepEval (juez LLM de la API) | `ai/deepeval/` | `pytest` (XWiki + API key) |
| **IA**: Stagehand (navegador dirigido por LLM) | `ai/stagehand/` | `npm run verificar` (XWiki + API key) |
| **IA**: generador de casos frontera (LLM) | `ai/generador-casos/` | `node genera-casos-frontera.mjs` |

Detalle de diseño de las pruebas (caminos, dobles, TDD/BDD): **`PRUEBAS-OBJECTS.md`**.

## Atajo: todo lo local en un comando

```powershell
cd tests\vyv-objects
.\correr-objects.ps1          # unitarias + BDD (no necesita worktree ni Docker)
.\correr-objects.ps1 sonar -token sqp_TU_TOKEN   # cobertura + quality gate propio
```

> Si `mvn` no está en el PATH: instala Maven 3.9+ o usa una copia portable y
> llama `...\apache-maven-3.9.9\bin\mvn.cmd` con los mismos argumentos.

## Requisitos por tipo de prueba

| Prueba | Necesita |
|---|---|
| Unitarias y BDD | JDK 17/21 (23 funciona), Maven 3.9+, internet la 1ª vez (baja el release 18.4.0 de nexus.xwiki.org) |
| E2E / UX / IA | `docker compose up -d` en la raíz + flavor de XWiki instalado (asistente en `http://localhost:8080`), Node 18+ |
| Sonar | worktree `xwiki-184` (`..\vyv-rest\setup.ps1`) + SonarQube en `localhost:9000` |
| DeepEval / Stagehand / generador | API key (`OPENAI_API_KEY`) |

## SonarQube y quality gate

`correr-objects.ps1 sonar` sube el proyecto **`Xwiki-objects`** (projectKey
propio para no pisar el dashboard combinado `Xwiki` del equipo) con:

- `sonar.inclusions=**/objects/ObjectResourceImpl.java` → el análisis y la
  cobertura se miden **solo sobre nuestra clase**;
- cobertura JaCoCo generada por `ObjectResourceImplTest`: los 13 casos cubren
  **todas las líneas y todas las ramas** de los 3 métodos (éxitos, 401, 404,
  revisión menor y excepciones), por lo que el quality gate de cobertura queda
  al máximo;
- las pruebas no añaden código de producción → cero code smells/bugs nuevos.

Para sumar a la nota grupal sigue funcionando el flujo del equipo:
`..\vyv-rest\correr.ps1 equipo` ya ejecuta `ObjectResourceImplTest` (el patrón
`$teamTests` lo incluye desde el reparto inicial).
