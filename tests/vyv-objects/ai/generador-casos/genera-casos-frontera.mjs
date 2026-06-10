/**
 * Herramienta IA adicional: generador de casos frontera con un LLM (API de OpenAI).
 *
 * Le entrega al modelo el codigo REAL de ObjectResourceImpl (release 18.4.0) y
 * la lista de tests existentes, y le pide proponer casos frontera que falten
 * (analisis estatico asistido por IA, complementa el diseno TDD de la suite).
 *
 * Requisitos: node >= 18, OPENAI_API_KEY exportada (la misma que usa DeepEval).
 * Ejecutar:   node genera-casos-frontera.mjs
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const aqui = dirname(fileURLToPath(import.meta.url));
const RUTA_TEST = join(
  aqui,
  '../../../vyv-rest/proyecto-modulo-real/src/test/java/org/xwiki/rest/internal/resources/objects/ObjectResourceImplTest.java'
);
const URL_FUENTE =
  'https://raw.githubusercontent.com/xwiki/xwiki-platform/xwiki-platform-18.4.0/xwiki-platform-core/xwiki-platform-rest/xwiki-platform-rest-server/src/main/java/org/xwiki/rest/internal/resources/objects/ObjectResourceImpl.java';

const apiKey = process.env.OPENAI_API_KEY;
if (!apiKey) {
  console.error('Falta OPENAI_API_KEY. Exportala y vuelve a ejecutar.');
  process.exit(1);
}

const fuente = await (await fetch(URL_FUENTE)).text();
const testsActuales = readFileSync(RUTA_TEST, 'utf8');

const respuesta = await fetch('https://api.openai.com/v1/chat/completions', {
  method: 'POST',
  headers: {
    Authorization: `Bearer ${apiKey}`,
    'content-type': 'application/json'
  },
  body: JSON.stringify({
    model: 'gpt-4o',
    max_tokens: 2000,
    messages: [
      {
        role: 'user',
        content:
          'Eres un experto en V&V de software. Esta es la clase bajo prueba:\n\n' +
          '```java\n' + fuente + '\n```\n\n' +
          'Y esta es la suite de pruebas unitarias actual:\n\n' +
          '```java\n' + testsActuales + '\n```\n\n' +
          'Enumera (en espanol, maximo 10) los casos frontera o de riesgo que ' +
          'NO esten cubiertos todavia, ordenados por valor. Para cada uno: ' +
          'nombre de test sugerido (patron metodo_Caso_Resultado), que prepara ' +
          'el Arrange y que se afirma en el Assert. Si la cobertura ya es ' +
          'completa, dilo explicitamente y sugiere pruebas de mutacion.'
      }
    ]
  })
});

if (!respuesta.ok) {
  console.error(`Error de la API del LLM: HTTP ${respuesta.status}`);
  console.error(await respuesta.text());
  process.exit(1);
}

const datos = await respuesta.json();
console.log('=== Casos frontera propuestos por la IA ===\n');
console.log(datos.choices?.[0]?.message?.content ?? '(respuesta vacia)');
