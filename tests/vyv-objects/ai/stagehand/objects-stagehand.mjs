/**
 * Verificacion E2E asistida por IA con Stagehand (navegador dirigido por LLM).
 *
 * Flujo: crea via API REST un objeto TagClass en una pagina, abre XWiki en un
 * navegador real, inicia sesion usando instrucciones en lenguaje natural
 * (act), y extrae con IA (extract) las etiquetas visibles para comprobar que
 * el objeto administrado por ObjectResourceImpl se refleja en la interfaz.
 *
 * Requisitos:
 *   - XWiki en http://localhost:8080 (docker compose up -d, flavor instalado)
 *   - npm install
 *   - API key del modelo: GROQ_API_KEY (gratis, Llama) u OPENAI_API_KEY.
 *     PowerShell:  $env:GROQ_API_KEY = "gsk_..."   (NUNCA commitear la key)
 *
 * Ejecutar:  npm run verificar
 */
import { Stagehand } from '@browserbasehq/stagehand';
import { z } from 'zod';

const BASE = process.env.XWIKI_URL ?? 'http://localhost:8080';
const USUARIO = process.env.XWIKI_USER ?? 'Admin';
const CLAVE = process.env.XWIKI_PASS ?? 'admin';
const ESPACIO = 'VyVObjectsIA';
const PAGINA = 'PruebaStagehand';
const URL_PAGINA = `${BASE}/rest/wikis/xwiki/spaces/${ESPACIO}/pages/${PAGINA}`;
const ETIQUETA = 'stagehand-vyv';

const auth = 'Basic ' + Buffer.from(`${USUARIO}:${CLAVE}`).toString('base64');

async function prepararDatos() {
  await fetch(URL_PAGINA, {
    method: 'PUT',
    headers: { Authorization: auth, 'Content-Type': 'application/xml' },
    body: `<?xml version="1.0" encoding="UTF-8"?>
<page xmlns="http://www.xwiki.org"><title>Prueba Stagehand</title><content>.</content></page>`
  });
  const creado = await fetch(`${URL_PAGINA}/objects`, {
    method: 'POST',
    headers: {
      Authorization: auth,
      'Content-Type': 'application/x-www-form-urlencoded',
      Accept: 'application/json'
    },
    body: `className=XWiki.TagClass&${encodeURIComponent('property#tags')}=${ETIQUETA}`
  });
  if (creado.status !== 201) {
    throw new Error(`No se pudo crear el objeto de prueba (HTTP ${creado.status})`);
  }
}

async function limpiarDatos() {
  await fetch(URL_PAGINA, { method: 'DELETE', headers: { Authorization: auth } });
}

// Groq expone API compatible con OpenAI; si hay GROQ_API_KEY se usa su modelo Llama.
const stagehand = new Stagehand(
  process.env.GROQ_API_KEY
    ? {
        env: 'LOCAL',
        verbose: 1,
        modelName: 'groq/llama-3.3-70b-versatile',
        modelClientOptions: { apiKey: process.env.GROQ_API_KEY }
      }
    : { env: 'LOCAL', verbose: 1 }
);

try {
  await prepararDatos();
  await stagehand.init();
  const pagina = stagehand.page;

  await pagina.goto(`${BASE}/bin/login/XWiki/XWikiLogin`);
  await pagina.act(`Escribe "${USUARIO}" en el campo de usuario`);
  await pagina.act(`Escribe "${CLAVE}" en el campo de contrasena`);
  await pagina.act('Haz clic en el boton para iniciar sesion');

  await pagina.goto(`${BASE}/bin/view/${ESPACIO}/${PAGINA}`);
  const resultado = await pagina.extract({
    instruction:
      'Extrae el titulo de la pagina y la lista de etiquetas (tags) visibles en ella',
    schema: z.object({
      titulo: z.string(),
      etiquetas: z.array(z.string())
    })
  });

  console.log('Extraido por la IA:', JSON.stringify(resultado, null, 2));

  if (!resultado.etiquetas.some((t) => t.includes(ETIQUETA))) {
    throw new Error(
      `FALLO: la etiqueta "${ETIQUETA}" (objeto creado via ObjectResourceImpl) no se ve en la interfaz`
    );
  }
  console.log('OK: el objeto creado por la API REST se refleja en la interfaz de XWiki.');
} finally {
  await stagehand.close().catch(() => {});
  await limpiarDatos().catch(() => {});
}
