# tests/ — Pruebas V&V sobre XWiki

Pruebas unitarias del equipo contra el **código real de XWiki**, con cobertura en
**SonarQube**, sin compilar el monorepo completo.

```
tests/
└── vyv-rest/          <- todo el montaje (tests + scripts + guías)
    ├── README-EQUIPO.md   <- GUÍA PASO A PASO (léela)
    ├── CONTEXTO.md        <- el "por qué" (snapshot, worktree, etc.)
    ├── setup.ps1          <- prepara el entorno (1 vez)
    ├── correr.ps1         <- corre tests / sonar / equipo
    ├── settings-xwiki.xml <- config Maven (repos nexus)
    └── proyecto-modulo-real/  <- aquí van los tests
```

---

## Arranque rápido

```powershell
git pull
cd tests\vyv-rest
.\setup.ps1                              # 1 vez: crea el worktree ../xwiki-184
.\correr.ps1                            # corre tus tests
.\correr.ps1 equipo -token sqp_TU_TOKEN # todos los tests + cobertura combinada en Sonar
```

> Si PowerShell bloquea scripts:
> `powershell -ExecutionPolicy Bypass -File .\setup.ps1`

---

## Reparto de funcionalidades (todas en `xwiki-platform-rest-server`)

| Persona | Funcionalidad | Clase real | Package del test |
|---|---|---|---|
| (tú) | comments | `CommentResourceImpl` (+3) | `org.xwiki.rest.internal.resources.comments` |
| Compa 1 | objects | `ObjectResourceImpl` | `org.xwiki.rest.internal.resources.objects` |
| Compa 2 | attachments | `AttachmentResourceImpl` | `org.xwiki.rest.internal.resources.attachments` |
| Compa 3 | page tags | `PageTagsResourceImpl` | `org.xwiki.rest.internal.resources.pages` |

Cada quien crea su test en:
```
vyv-rest/proyecto-modulo-real/src/test/java/org/xwiki/rest/internal/resources/<su-paquete>/
```
con el `package` igual al de la clase real.

---

## Idea en 3 frases

1. `master` es `18.5.0-SNAPSHOT` y **no compila** (su POM padre no está publicado).
2. Por eso usamos un **worktree** `../xwiki-184` con el release **18.4.0**, que sí compila;
   `setup.ps1` lo crea (no viaja por `git pull`).
3. Tus tests viven en este repo; `correr.ps1` los copia al worktree y los ejecuta. Para la
   cobertura del equipo en un solo proyecto Sonar: `correr.ps1 equipo`.

**Detalle completo en [vyv-rest/README-EQUIPO.md](vyv-rest/README-EQUIPO.md) y [vyv-rest/CONTEXTO.md](vyv-rest/CONTEXTO.md).**
