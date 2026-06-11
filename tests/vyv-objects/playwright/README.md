# Playwright E2E — funcionalidad "objects"

Pruebas E2E con **Playwright Test** de `ObjectResourceImpl` contra el XWiki real
de `docker-compose`. Complementan a Cypress con un segundo motor de navegador.

## Specs

| Archivo | Qué prueba |
|---|---|
| `tests/objetos-api.spec.js` | Ciclo REST con el `request` fixture: GET 200, GET 404, PUT 202, PUT 401, DELETE + GET 404 |
| `tests/objetos-ui.spec.js` | Login real + la etiqueta del objeto visible en la página y el objeto en el editor de objetos |

## Requisitos y ejecución

```powershell
# XWiki de docker-compose arriba en localhost:8080
cd tests\vyv-objects\playwright
npm install
npx playwright install chromium    # una vez

$env:XWIKI_USER = "Admin"; $env:XWIKI_PASS = "admin"
npx playwright test                # reporte HTML en playwright-report/
```

Variables de entorno:
- `XWIKI_URL` (default `http://localhost:8080`), `XWIKI_USER`, `XWIKI_PASS`.
- `PW_CHROME_PATH`: ruta a un Chromium alterno (p. ej. el *headless shell* de
  Playwright) en máquinas donde el antivirus bloquea el `chrome.exe` por defecto:
  `%LOCALAPPDATA%\ms-playwright\chromium_headless_shell-1223\chrome-headless-shell-win64\chrome-headless-shell.exe`
