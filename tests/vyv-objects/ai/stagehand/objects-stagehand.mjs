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
      'Content-Type': 'application/xml',
      Accept: 'application/json'
    },
    body:
      `<?xml version="1.0" encoding="UTF-8"?>` +
      `<object xmlns="http://www.xwiki.org"><className>XWiki.TagClass</className>` +
      `<property name="tags"><value>${ETIQUETA}</value></property></object>`
  });
  if (creado.status !== 201) {
    throw new Error(`No se pudo crear el objeto de prueba (HTTP ${creado.status})`);
  }
}

async function limpiarDatos() {
  await fetch(URL_PAGINA, { method: 'DELETE', headers: { Authorization: auth } });
}

// Groq expone API compatible con OpenAI; si hay GROQ_API_KEY se usa su modelo Llama.
// STAGEHAND_CHROME_PATH permite apuntar a otro binario de Chromium (p. ej. el
// headless shell de Playwright) en maquinas donde el antivirus bloquea chrome.exe.
const opcionesNavegador = process.env.STAGEHAND_CHROME_PATH
  ? { localBrowserLaunchOptions: { executablePath: process.env.STAGEHAND_CHROME_PATH, headless: true } }
  : {};
const stagehand = new Stagehand(
  process.env.GROQ_API_KEY
    ? {
        env: 'LOCAL',
        verbose: 1,
        modelName: 'groq/llama-3.3-70b-versatile',
        modelClientOptions: { apiKey: process.env.GROQ_API_KEY },
        ...opcionesNavegador
      }
    : { env: 'LOCAL', verbose: 1, ...opcionesNavegador }
);

try {
  await prepararDatos();
  await stagehand.init();
  const pagina = stagehand.page;

  await pagina.goto(`${BASE}/bin/login/XWiki/XWikiLogin`);
  await pagina.act(`Escribe "${USUARIO}" en el campo de usuario`);
  await pagina.act(`Escribe "${CLAVE}" en el campo de contrasena`);
  await pagina.act('Haz clic en el boton para iniciar sesion');

  // El editor de objetos es la vista de la interfaz donde se administra lo que
  // gestiona ObjectResourceImpl: alli debe aparecer el objeto XWiki.TagClass.
  await pagina.goto(`${BASE}/bin/edit/${ESPACIO}/${PAGINA}?editor=object`);
  const resultado = await pagina.extract({
    instruction:
      'Extrae el nombre de la pagina que se esta editando y la lista de clases de objetos presentes en el editor de objetos',
    schema: z.object({
      titulo: z.string(),
      clasesDeObjetos: z.array(z.string())
    })
  });

  console.log('Extraido por la IA:', JSON.stringify(resultado, null, 2));

  const iaLoVio = resultado.clasesDeObjetos.some((c) => c.includes('TagClass'));
  if (!iaLoVio) {
    // Respaldo determinista: verificar el DOM directamente
    const html = await pagina.content();
    if (!html.includes('TagClass')) {
      throw new Error(
        'FALLO: el objeto XWiki.TagClass (creado via ObjectResourceImpl) no aparece en el editor de objetos'
      );
    }
    console.log('AVISO: la IA no lo extrajo, pero el DOM del editor SI contiene XWiki.TagClass.');
  }
  console.log('OK: el objeto creado por la API REST se refleja en la interfaz de XWiki (editor de objetos).');
} finally {
  await stagehand.close().catch(() => {});
  await limpiarDatos().catch(() => {});
}
