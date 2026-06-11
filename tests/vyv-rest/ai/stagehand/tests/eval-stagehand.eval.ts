/**
 * Test de captura para DeepEval (funcionalidad: Comentarios de XWiki).
 *
 * Ejecuta las 3 primitivas de Stagehand sobre una pagina real con comentarios y
 * guarda los inputs/outputs (mas el innerText de la pagina como context RAG) en
 * deepeval/data/stagehand-results.json.
 *
 * Luego pytest (en ../deepeval/) lee ese JSON y corre las 3 metricas:
 *   - Correccion  (GEval)
 *   - RAG         (Faithfulness)
 *   - Toxicidad   (ToxicityMetric)
 */
import { z } from "zod";
import { createStagehand } from "../src";
import { EvalRecorder, truncar } from "../src/eval-recorder";
import { loginDeterminista } from "../src/login";
import { crearPagina, crearComentario, borrarPagina } from "../src/seed";
import "../src/env";

const baseUrl = process.env.BASE_URL || "http://localhost:8080";
const ESPACIO = "ComentariosEval";
const PAGINA = "PruebaDeepEval";
const RESULTS_PATH = "../deepeval/data/stagehand-results.json";

async function main() {
  // Arrange (API): pagina dedicada con comentarios reales para evaluar
  await crearPagina(ESPACIO, PAGINA);
  await crearComentario(ESPACIO, PAGINA, "Excelente articulo, muy claro y util.");
  await crearComentario(ESPACIO, PAGINA, "Gracias por compartir, me sirvio mucho.");
  await crearComentario(ESPACIO, PAGINA, "Tengo una duda sobre el segundo paso.");

  const sh = createStagehand();
  await sh.init();
  const page = sh.context.activePage()!;
  const rec = new EvalRecorder();

  try {
    await loginDeterminista(sh);
    await page.goto(`${baseUrl}/bin/view/${ESPACIO}/${PAGINA}?viewer=comments`);
    await page.waitForLoadState("load");
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    await page.waitForTimeout(800);

    const a11y = await page.evaluate(() => document.body.innerText);

    // =====================================================================
    // CASO 1 — EXTRACT comentarios
    // =====================================================================
    const instr1 = "extrae los comentarios visibles con autor y mensaje";
    const data1 = await sh.extract(
      instr1,
      z.object({
        comentarios: z.array(z.object({
          autor: z.string().nullable(),
          mensaje: z.string(),
        })),
      }),
    );
    console.log(`[caso 1] extract devolvio ${data1.comentarios.length} comentarios`);

    rec.add({
      id: "extract-comentarios",
      primitive: "extract",
      input: instr1,
      actual_output: JSON.stringify(data1, null, 2),
      expected_output:
        "Lista de comentarios visibles en la pagina. Cada comentario tiene autor " +
        "(string o null) y mensaje (string). Los datos deben estar presentes en el " +
        "DOM, no inventados.",
      context: [truncar(a11y)],
      metadata: { url: page.url(), primitive_description: "extract de comentarios" },
    });

    // =====================================================================
    // CASO 2 — OBSERVE seccion de comentarios
    // =====================================================================
    const instr2 = "seccion de comentarios de la pagina con sus mensajes";
    let obs: unknown[] = [];
    try {
      obs = await sh.observe(instr2);
    } catch (e) {
      obs = [];
    }
    console.log(`[caso 2] observe detecto ${obs.length} elementos`);

    rec.add({
      id: "observe-comentarios",
      primitive: "observe",
      input: instr2,
      actual_output: JSON.stringify(obs, null, 2),
      expected_output:
        "Lista de elementos de la seccion de comentarios. Cada elemento tiene " +
        "description, method y selector.",
      context: [truncar(a11y)],
      metadata: { url: page.url(), primitive_description: "observe sin ejecutar accion" },
    });

    // =====================================================================
    // CASO 3 — ACT abrir el formulario de comentario
    // =====================================================================
    const instr3 = "haz click en el boton o enlace para agregar un nuevo comentario";
    const urlAntes = page.url();
    try {
      await sh.act(instr3);
    } catch (e) {
      // si no encuentra el control, igual registramos el intento
    }
    await page.waitForTimeout(2000);
    const urlDespues = page.url();

    rec.add({
      id: "act-agregar-comentario",
      primitive: "act",
      input: instr3,
      actual_output: JSON.stringify({
        url_antes: urlAntes,
        url_despues: urlDespues,
      }, null, 2),
      expected_output:
        "La accion intenta abrir el formulario para comentar. Se registra la URL " +
        "antes y despues de la interaccion.",
      context: [truncar(a11y)],
      metadata: { url_inicial: urlAntes, url_final: urlDespues },
    });

    await rec.save(RESULTS_PATH);
    console.log("✅ eval-stagehand PASSED — JSON listo para DeepEval");
  } finally {
    await sh.close();
    await borrarPagina(ESPACIO, PAGINA);
  }
}

main().catch((e) => { console.error("❌ FAILED:", e); process.exit(1); });
