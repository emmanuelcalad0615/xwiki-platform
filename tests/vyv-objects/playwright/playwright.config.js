// @ts-check
const { defineConfig } = require('@playwright/test');

/**
 * Config de Playwright Test para la funcionalidad "objects".
 * Requiere el XWiki de docker-compose en http://localhost:8080.
 *
 * Variables de entorno (con defaults para el entorno local):
 *   XWIKI_URL, XWIKI_USER, XWIKI_PASS
 *   PW_CHROME_PATH -> ruta a un Chromium alterno (p.ej. el headless shell) en
 *   maquinas donde el antivirus bloquea el chrome.exe por defecto.
 */
const chromePath = process.env.PW_CHROME_PATH;

module.exports = defineConfig({
  testDir: './tests',
  timeout: 60000,
  fullyParallel: false,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  use: {
    baseURL: process.env.XWIKI_URL || 'http://localhost:8080',
    ignoreHTTPSErrors: true,
    screenshot: 'only-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: {
        browserName: 'chromium',
        ...(chromePath ? { launchOptions: { executablePath: chromePath } } : {})
      }
    }
  ]
});
