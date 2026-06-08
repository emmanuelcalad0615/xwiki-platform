# Pruebas V&V sobre XWiki REST — Guía del equipo

Pruebas unitarias (JUnit 5 + Mockito) contra el **código real de XWiki**, con cobertura en
**SonarQube**, **sin compilar el monorepo completo**. Trabajo de 4 personas, una
funcionalidad cada una, todas en el módulo `xwiki-platform-rest-server`.

> ¿Quieres el "por qué" de todo (snapshot, worktree, etc.)? Lee **CONTEXTO.md**.
> Esta guía es el "cómo".

---

## 1. Reparto de funcionalidades

| Persona | Funcionalidad | Clase real | Package del test |
|---|---|---|---|
| (tú) | comments | `CommentResourceImpl` (+3) | `org.xwiki.rest.internal.resources.comments` |
| Compa 1 | objects | `ObjectResourceImpl` | `org.xwiki.rest.internal.resources.objects` |
| Compa 2 | attachments | `AttachmentResourceImpl` | `org.xwiki.rest.internal.resources.attachments` |
| Compa 3 | page tags | `PageTagsResourceImpl` | `org.xwiki.rest.internal.resources.pages` |

Las 4 viven en el mismo módulo → se pueden analizar **juntas** (cobertura combinada).

---

## 2. Requisitos (instalar una vez por PC)

| Herramienta | Versión | Para qué |
|---|---|---|
| Git | cualquiera | clonar + worktree |
| JDK | **17 o 21** | compilar (24 puede romper) |
| Maven | 3.9+ | build |
| SonarQube | corriendo en `localhost:9000` | solo modo `sonar` / `equipo` |
| Internet | — | 1ª vez baja el tag + dependencias (~GB) |
| Disco | ~20 GB libres | worktree + `~/.m2` |

Verifica: `java -version`, `mvn -version`, y abre `http://localhost:9000`.

---

## 3. Setup (UNA sola vez, tras clonar/pull)

```powershell
git pull
cd tests\vyv-rest
.\setup.ps1
```

`setup.ps1` hace 3 cosas:
1. Configura Maven con el repo de XWiki (`~/.m2/settings.xml`).
2. Baja el tag release **18.4.0** de XWiki.
3. Crea el worktree **`../xwiki-184`** (carpeta hermana, donde SÍ compila).

> Si PowerShell bloquea scripts:
> `powershell -ExecutionPolicy Bypass -File .\setup.ps1`

Estructura que queda:
```
projects\
├── xwiki-platform\   <- ESTE repo (tus tests, scripts). Aquí editas y commiteas.
└── xwiki-184\        <- worktree release 18.4.0. Lo crea setup.ps1. NO se commitea.
```

---

## 4. Dónde crea cada uno su test

> **Las plantillas YA están creadas, vacías.** Cada compa abre la suya y la implementa.
> - `comments/CommentResourceImplTest.java` (+3) — **completa** (úsala de ejemplo/referencia)
> - `objects/ObjectResourceImplTest.java` — vacía (esqueleto + `@Disabled`)
> - `attachments/AttachmentResourceImplTest.java` — vacía (esqueleto + `@Disabled`)
> - `pages/PageTagsResourceImplTest.java` — vacía (esqueleto + `@Disabled`)
>
> Cada plantilla tiene el `package` correcto, `@ComponentTest`, el campo `@InjectMockComponents`
> y un método `pendiente()` anotado con `@Disabled`. **Implementa tus casos y quita `@Disabled`.**
> Mientras esté `@Disabled`, ese test sale como *skipped* (no rompe el build) y la clase
> aparece con **0% de cobertura** hasta que la cubras.

En el repo compartido, respetando el package de la clase:

```
tests/vyv-rest/proyecto-modulo-real/src/test/java/org/xwiki/rest/internal/resources/
├── comments/CommentResourceImplTest.java          (tú)
├── objects/ObjectResourceImplTest.java             (Compa 1)
├── attachments/AttachmentResourceImplTest.java     (Compa 2)
└── pages/PageTagsResourceImplTest.java             (Compa 3)
```

⚠️ La 1ª línea del test (`package ...`) DEBE coincidir con la de la clase real:

| Test | package |
|---|---|
| ObjectResourceImplTest | `package org.xwiki.rest.internal.resources.objects;` |
| AttachmentResourceImplTest | `package org.xwiki.rest.internal.resources.attachments;` |
| PageTagsResourceImplTest | `package org.xwiki.rest.internal.resources.pages;` |

### Estructura mínima de un test
```java
package org.xwiki.rest.internal.resources.<tu-paquete>;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ComponentTest
class TuClaseImplTest
{
    @InjectMockComponents
    private TuClaseImpl sujeto;   // la clase REAL de XWiki, inyectada

    @Test
    void metodo_Caso_ResultadoEsperado() throws Exception
    {
        // Arrange  (mocks/stubs de las dependencias)
        // Act      (llamar sujeto.metodo(...))
        // Assert   (assertEquals / assertThrows / verify)
    }
}
```
Usa `comments/CommentResourceImplTest.java` como ejemplo completo (mockStatic, FieldUtils, los 5 dobles).

---

## 5. Correr

Desde `tests\vyv-rest`:

### Solo tus tests (rápido, mientras desarrollas)
```powershell
.\correr.ps1
```

### Tu funcionalidad con cobertura + Sonar (proyecto propio)
```powershell
.\correr.ps1 sonar -projectKey Xwiki-attachments -token sqp_TU_TOKEN
```

### TODO el equipo junto (cobertura combinada) ← el importante
```powershell
.\correr.ps1 equipo -token sqp_TU_TOKEN
```
Esto sincroniza los tests de los 4, corre **un solo** análisis y sube **un** proyecto
`Xwiki` con la cobertura combinada → `http://localhost:9000/dashboard?id=Xwiki`

> Token: pásalo con `-token`, o ejecuta antes `$env:SONAR_TOKEN = "sqp_..."`.

---

## 6. Cobertura — cómo funciona y cómo NO pisarse

### Qué mide la cobertura
Cobertura = líneas de `src/main` ejercitadas por los tests ÷ líneas totales analizadas.

- **A nivel de TU clase**: depende de la **calidad** de tu test, no de cuántas funcionalidades
  haya. Un test que recorre `getX`, casos de error y permisos → tu clase sale **verde**.
  Una plantilla `@Disabled` o un test trivial → tu clase sale **roja (0%)**.
  👉 Hacer "una sola funcionalidad" NO baja la cobertura; lo que la baja es no implementar
  tests reales para esa clase.
- **A nivel de PROYECTO**: por defecto el módulo `rest-server` ya trae los tests propios de
  XWiki, así que el % global es alto aunque ustedes aporten poco. Por eso, para reflejar **lo
  que el EQUIPO aporta**, `correr.ps1 equipo` limita el análisis a **las clases del equipo**
  con `sonar.inclusions` (ver `$teamInclusions` en `correr.ps1`).

### Cómo NO pisarse en Sonar
Sonar sobrescribe si cada quien sube por separado con el mismo `projectKey`. Para sumar:

- **Un solo análisis con todos los tests** → usar `correr.ps1 equipo` (recomendado).
  - Requisito: todos pushearon sus tests, y quien corre `equipo` hizo `git pull` para tenerlos.
  - Sube **un** proyecto `Xwiki` con la cobertura **solo de las 4 clases del equipo**.
- Si prefieren dashboards separados: cada quien `correr.ps1 sonar -projectKey Xwiki-<func>`.

### Importante para la nota
- Mientras una plantilla siga `@Disabled`, esa clase aparece en **0%** → impleméntenla.
- Si cambian de funcionalidades, actualicen la lista `$teamInclusions` en `correr.ps1`.

---

## 7. Flujo de trabajo diario

```
Editas tu test           ->  .\correr.ps1            ->  git add/commit/push
(en proyecto-modulo-real)    (corre en xwiki-184)        (en xwiki-platform)
```

Para la cobertura del equipo:
```
Todos pushean -> uno hace git pull -> .\correr.ps1 equipo -token sqp_...
```

El worktree `xwiki-184` **nunca** se edita a mano ni se commitea: el script sincroniza solo.

---

## 8. Problemas comunes

| Error | Causa | Solución |
|---|---|---|
| `.\correr.ps1 no se reconoce` | lo corres fuera de la carpeta | `cd tests\vyv-rest` primero |
| `Falta el worktree xwiki-184` | no corriste setup | `.\setup.ps1` |
| `Non-resolvable parent POM ...SNAPSHOT` | corriste sobre master | usa los scripts (apuntan al worktree) |
| `Plugin not found ...importmap` | falta repo nexus | `setup.ps1` crea `~/.m2/settings.xml`; si ya tenías uno, fusiónalo con `settings-xwiki.xml` |
| `Falta token de Sonar` | modo sonar/equipo sin token | `-token sqp_...` o `$env:SONAR_TOKEN` |
| `license check failed` | corriste `verify` a mano | usa `correr.ps1` (no usa `verify`) |
| `Maven session does not declare a top level project` | corriste sonar con `-pl` a mano | usa `correr.ps1` (corre desde el pom del módulo) |
| script bloqueado | ExecutionPolicy | `powershell -ExecutionPolicy Bypass -File .\correr.ps1 equipo` |

---

## 9. Reglas de oro

1. Edita tests SOLO en `proyecto-modulo-real/src/test/...`. Nunca en `xwiki-184`.
2. El `package` del test = el `package` de la clase real.
3. Commit/push SOLO en `xwiki-platform`. `xwiki-184` no se commitea.
4. NO commitees tokens (usa `-token` o `$env:SONAR_TOKEN`).
5. Para la nota del equipo: `correr.ps1 equipo`.
