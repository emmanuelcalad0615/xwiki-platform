# Contexto: cómo y por qué probamos XWiki así

Documento explicativo de TODO el montaje. Si llegas nuevo al proyecto, léelo de arriba
a abajo y vas a entender qué es cada carpeta, cada script y por qué existen.

---

## 1. El objetivo

Escribir y ejecutar **pruebas unitarias (JUnit 5 + Mockito)** contra las clases **reales**
de XWiki (el código de producción, no copias falsas), y pasar el análisis de **SonarQube**
con **cobertura**.

---

## 2. El problema de raíz

XWiki es un **monorepo gigante** (uno de los proyectos Maven más grandes que existen).
La rama `master` de este repo está en versión **`18.5.0-SNAPSHOT`**.

### ¿Qué es un SNAPSHOT?
En Maven una versión es de dos tipos:

| Tipo | Ejemplo | Qué es |
|---|---|---|
| **RELEASE** | `18.4.0` | versión final, **congelada y publicada**. Nunca cambia. |
| **SNAPSHOT** | `18.5.0-SNAPSHOT` | **borrador en desarrollo**, cambia a diario, casi nunca publicado. |

Analogía: RELEASE = libro publicado en librerías; SNAPSHOT = borrador del autor.

### Por qué `master` NO compila
`master` (18.5.0-SNAPSHOT) necesita su "POM padre" `xwiki-commons-pom:18.5.0-SNAPSHOT`,
que **no está publicado** en internet (vive en otro repo, `xwiki-commons`, y solo en las
máquinas de los devs de XWiki). Resultado al intentar compilar:

```
Non-resolvable parent POM ... xwiki-commons-pom:18.5.0-SNAPSHOT (absent)
```

Compilarlo "a la fuerza" implicaría construir antes `xwiki-commons` + `xwiki-rendering`
desde cero: **horas y varios GB**. Inviable para un trabajo de clase.

---

## 3. La solución

Usar la versión **RELEASE 18.4.0** (que SÍ está publicada) en una carpeta aparte, y dejar
tu repo intacto.

### ¿Qué es un "worktree"?
Git permite tener **varias carpetas del mismo repo, cada una en una versión distinta, a la
vez**. Cada carpeta extra es un **worktree**.

Analogía: tu repo es un libro; normalmente lo tienes abierto en una página (master). Un
worktree = abrir una **segunda copia del mismo libro en otra página** (18.4.0) sin cerrar
la primera. Mismo historial (`.git`), distintos archivos en disco.

### Las dos carpetas (hermanas)
"Hermanas" = dentro del mismo padre, lado a lado (no una dentro de otra).

```
projects\                  <- carpeta padre
├── xwiki-platform\        <- TU repo (rama master, 18.5.0-SNAPSHOT). NO compila.
│   └── tests\vyv-rest\          <- tus tests, scripts y docs (esto SÍ se commitea)
└── xwiki-184\             <- worktree (release 18.4.0). SÍ compila. NO se commitea.
```

`xwiki-184` se llama así por **18.4.0**. Es código de XWiki en versión compilable.

### Por qué 18.4.0 sí compila
Al ser RELEASE, todas sus dependencias (commons, rendering, etc.) están **publicadas** en
el repositorio público de XWiki (nexus.xwiki.org) y se descargan como `.jar` ya hechos.
No hay que compilar el monorepo: solo el módulo que te interesa.

---

## 4. Piezas del montaje

| Pieza | Qué es | ¿Se commitea? |
|---|---|---|
| `tests/vyv-rest/proyecto-modulo-real/` | tus tests (fuente de verdad) | ✅ sí |
| `settings-xwiki.xml` | apunta Maven al repo nexus de XWiki (si no, no encuentra plugins) | ✅ sí |
| `setup.ps1` | crea el worktree `xwiki-184` + configura Maven. Una sola vez. | ✅ sí |
| `correr.ps1` | copia tus tests al worktree y los ejecuta (tests o sonar) | ✅ sí |
| `README-EQUIPO.md` | guía rápida de pasos | ✅ sí |
| `xwiki-184/` | el worktree con XWiki 18.4.0 | ❌ no (lo crea setup.ps1) |
| `~/.m2/settings.xml` | copia global del settings (lo pone setup.ps1) | ❌ no (config local) |

### ¿Por qué `xwiki-184` no viaja por `git pull`?
Son ~14.000 archivos del release de XWiki: no son TU trabajo y no deben ir en tu repo.
En su lugar, `setup.ps1` lo baja y lo crea con un comando en cada PC.

---

## 5. El flujo de trabajo

```
Editas tests        ->  correr.ps1 (copia al worktree y corre)  ->  commit/push en TU repo
(en xwiki-platform)     (ejecuta en xwiki-184)                      (xwiki-184 nunca se toca a mano)
```

1. Editas/creas tests en `proyecto-modulo-real/src/test/.../<tu-funcionalidad>/`.
2. `.\correr.ps1` (o `.\correr.ps1 sonar`).
3. `git commit` + `git push` en `xwiki-platform`.

El script siempre **sincroniza** tus tests al worktree antes de correr, así que nunca
editas dentro de `xwiki-184`.

---

## 6. Cómo se hacen portables los scripts (sin rutas fijas)

No hay `C:\Users\thepi\...` hardcodeado. El script calcula las rutas desde su propia
ubicación (`$PSScriptRoot`):

```powershell
$here     = $PSScriptRoot                          # .../xwiki-platform/tests/vyv-rest
$repo     = (Resolve-Path "$here\..\..").Path      # sube 2 -> .../xwiki-platform
$worktree = Join-Path (Split-Path $repo -Parent) "xwiki-184"   # hermano -> .../xwiki-184
```

Por eso funciona con cualquier usuario y cualquier disco, **siempre que `xwiki-platform` y
`xwiki-184` sean carpetas hermanas** (lo que garantiza `setup.ps1`).

---

## 7. Adaptar a OTRA funcionalidad (para cada compañero)

El montaje sirve para cualquier recurso REST de XWiki. Solo hay que ajustar en `correr.ps1`:

1. `$origen` y `$destino` -> el **package** de tu clase
   (ej: `...\resources\attachments` en vez de `...\resources\comments`).
2. `$modulo` -> **solo si** tu clase vive en otro módulo de XWiki (no `xwiki-platform-rest-server`).
   Apúntalo a la ruta de ese módulo dentro de `xwiki-184`.
3. `-Dtest="..."` -> el patrón de tus tests (ej: `Attachment*Test`).

El resto (worktree, settings, jacoco, sonar) es idéntico.

> Cómo saber tu módulo: busca tu clase `.java` en `xwiki-184`, sube carpetas hasta el
> `pom.xml` con `<artifactId>`: esa carpeta es tu módulo.

---

## 8. SonarQube y cobertura (importante para el equipo)

Cada análisis de Sonar es una **foto completa** de lo que se escaneó. Si **todos** usan el
mismo `projectKey` y cada uno analiza su módulo por separado, **se sobreescriben** (el
último borra al anterior). NO se suma solo.

Recomendado: **cada compañero su propio `projectKey`**:
```powershell
.\correr.ps1 sonar -token sqp_TU_TOKEN   # y cambia projectKey en el script a Xwiki-<tu-func>
```
Así cada quien ve la cobertura de SUS clases sin pisar la del otro.

- La cobertura mide cuánto de `src/main` ejercitan los tests.
- Tus tests cubren **tus clases**. XWiki ya trae sus propios tests, así que la base ya es
  alta; lo tuyo suma sobre las clases de tu funcionalidad.
- Para una cobertura **agregada** real del proyecto habría que analizar todos los módulos
  en una sola corrida (build pesado).

---

## 9. Requisitos por PC (no vienen del repo)

| Herramienta | Versión | Para qué |
|---|---|---|
| Git | cualquiera | clonar + worktree |
| JDK | 17 o 21 | compilar (24 puede romper) |
| Maven | 3.9+ | build |
| SonarQube | en `localhost:9000` | solo modo `sonar` |
| Internet | — | 1ª vez baja tag + dependencias (~GB) |
| Disco | ~20 GB libres | worktree + `~/.m2` |

---

## 10. Resumen en 5 frases

1. `master` es un SNAPSHOT y no compila (falta su POM padre, no publicado).
2. Por eso usamos `xwiki-184`, un **worktree** con el release **18.4.0** que sí compila.
3. Tus tests viven en tu repo; `correr.ps1` los copia al worktree y los ejecuta.
4. `setup.ps1` crea ese worktree en cualquier PC (no viaja por git).
5. Sonar funciona sobre el módulo real; cada quien con su `projectKey` para no pisarse.
