// @ts-check
// UI E2E con Playwright Test de la funcionalidad "objects": el objeto creado por
// la API REST debe verse en la interfaz de XWiki (keyword del documento) y
// aparecer en el editor de objetos. Incluye el login real por formulario.
const { test, expect, request: apiRequest } = require('@playwright/test');

const WIKI = 'xwiki';
const ESPACIO = 'VyVObjectsPW';
const PAGINA = 'PruebaPlaywrightUI';
const CLASE = 'XWiki.TagClass';
const BASE = process.env.XWIKI_URL || 'http://localhost:8080';
const USER = process.env.XWIKI_USER || 'Admin';
const PASS = process.env.XWIKI_PASS || 'admin';
const AUTH = 'Basic ' + Buffer.from(`${USER}:${PASS}`).toString('base64');

const urlPagina = `/rest/wikis/${WIKI}/spaces/${ESPACIO}/pages/${PAGINA}`;
const ETIQUETA = 'etiqueta-playwright';

test.beforeAll(async () => {
  const ctx = await apiRequest.newContext({ baseURL: BASE });
  await ctx.put(urlPagina, {
    headers: { Authorization: AUTH, 'Content-Type': 'application/xml' },
    data: `<?xml version="1.0"?><page xmlns="http://www.xwiki.org"><title>UI PW objetos</title><content>Pagina UI Playwright.</content></page>`
  });
  await ctx.post(`${urlPagina}/objects`, {
    headers: { Authorization: AUTH, 'Content-Type': 'application/xml', Accept: 'application/json' },
    data:
      `<?xml version="1.0" encoding="UTF-8"?>` +
      `<object xmlns="http://www.xwiki.org"><className>${CLASE}</className>` +
      `<property name="tags"><value>${ETIQUETA}</value></property></object>`
  });
  await ctx.dispose();
});

test.afterAll(async () => {
  const ctx = await apiRequest.newContext({ baseURL: BASE });
  await ctx.delete(urlPagina, { headers: { Authorization: AUTH } });
  await ctx.dispose();
});

test('login + la etiqueta del objeto se ve en la pagina renderizada', async ({ page }) => {
  await page.goto('/bin/login/XWiki/XWikiLogin');
  await page.fill('#j_username', USER);
  await page.fill('#j_password', PASS);
  await page.click('input[type="submit"], button[type="submit"]');
  await page.waitForLoadState('networkidle');

  await page.goto(`/bin/view/${ESPACIO}/${PAGINA}`);
  const keywords = await page.getAttribute('meta[name="keywords"]', 'content');
  expect(keywords).toContain(ETIQUETA);
});

test('el objeto XWiki.TagClass aparece en el editor de objetos', async ({ page }) => {
  await page.goto('/bin/login/XWiki/XWikiLogin');
  await page.fill('#j_username', USER);
  await page.fill('#j_password', PASS);
  await page.click('input[type="submit"], button[type="submit"]');
  await page.waitForLoadState('networkidle');

  await page.goto(`/bin/edit/${ESPACIO}/${PAGINA}?editor=object`);
  await expect(page.locator('body')).toContainText('TagClass');
});
