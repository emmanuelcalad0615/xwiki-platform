// @ts-check
// API E2E con Playwright Test (request fixture) de la funcionalidad "objects".
// Cubre el ciclo de vida REST que implementa ObjectResourceImpl: getObject,
// updateObject y deleteObject, mas los caminos de error 404 y 401.
const { test, expect } = require('@playwright/test');

const WIKI = 'xwiki';
const ESPACIO = 'VyVObjectsPW';
const PAGINA = 'PruebaPlaywrightApi';
const CLASE = 'XWiki.TagClass';
const USER = process.env.XWIKI_USER || 'Admin';
const PASS = process.env.XWIKI_PASS || 'admin';
const AUTH = 'Basic ' + Buffer.from(`${USER}:${PASS}`).toString('base64');

const urlPagina = `/rest/wikis/${WIKI}/spaces/${ESPACIO}/pages/${PAGINA}`;
const urlObjetos = `${urlPagina}/objects`;

const xmlObjeto = (tags) =>
  `<?xml version="1.0" encoding="UTF-8"?>` +
  `<object xmlns="http://www.xwiki.org"><className>${CLASE}</className>` +
  `<property name="tags"><value>${tags}</value></property></object>`;

let numero;

test.beforeAll(async ({ request }) => {
  await request.put(urlPagina, {
    headers: { Authorization: AUTH, 'Content-Type': 'application/xml' },
    data: `<?xml version="1.0"?><page xmlns="http://www.xwiki.org"><title>API PW objetos</title><content>.</content></page>`
  });
  const creado = await request.post(urlObjetos, {
    headers: { Authorization: AUTH, 'Content-Type': 'application/xml', Accept: 'application/json' },
    data: xmlObjeto('playwright-api')
  });
  expect(creado.status()).toBe(201);
  numero = (await creado.json()).number;
});

test.afterAll(async ({ request }) => {
  await request.delete(urlPagina, { headers: { Authorization: AUTH } });
});

test('GET devuelve el objeto con su clase y numero (200)', async ({ request }) => {
  const res = await request.get(`${urlObjetos}/${CLASE}/${numero}`, {
    headers: { Authorization: AUTH, Accept: 'application/json' }
  });
  expect(res.status()).toBe(200);
  const body = await res.json();
  expect(body.className).toBe(CLASE);
  expect(body.number).toBe(numero);
});

test('GET de un objeto inexistente devuelve 404', async ({ request }) => {
  const res = await request.get(`${urlObjetos}/${CLASE}/9999`, {
    headers: { Authorization: AUTH }
  });
  expect(res.status()).toBe(404);
});

test('PUT actualiza el objeto (202)', async ({ request }) => {
  const res = await request.put(`${urlObjetos}/${CLASE}/${numero}`, {
    headers: { Authorization: AUTH, 'Content-Type': 'application/xml', Accept: 'application/json' },
    data: xmlObjeto('playwright-actualizado')
  });
  expect(res.status()).toBe(202);
  const tags = (await res.json()).properties.find((p) => p.name === 'tags');
  expect(tags.value).toBe('playwright-actualizado');
});

test('PUT sin credenciales devuelve 401 y no modifica el objeto', async ({ request }) => {
  const res = await request.put(`${urlObjetos}/${CLASE}/${numero}`, {
    headers: { 'Content-Type': 'application/xml' },
    data: xmlObjeto('intruso')
  });
  expect(res.status()).toBe(401);
  const verif = await request.get(`${urlObjetos}/${CLASE}/${numero}`, {
    headers: { Authorization: AUTH, Accept: 'application/json' }
  });
  const tags = (await verif.json()).properties.find((p) => p.name === 'tags');
  expect(tags.value).toBe('playwright-actualizado');
});

test('DELETE elimina el objeto y el GET posterior da 404', async ({ request }) => {
  const creado = await request.post(urlObjetos, {
    headers: { Authorization: AUTH, 'Content-Type': 'application/xml', Accept: 'application/json' },
    data: xmlObjeto('para-borrar')
  });
  const n = (await creado.json()).number;
  const del = await request.delete(`${urlObjetos}/${CLASE}/${n}`, { headers: { Authorization: AUTH } });
  expect([200, 204]).toContain(del.status());
  const verif = await request.get(`${urlObjetos}/${CLASE}/${n}`, { headers: { Authorization: AUTH } });
  expect(verif.status()).toBe(404);
});
