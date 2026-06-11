import { z } from "zod";
import { createStagehand } from "../src";
import { loginConIA } from "../src/login";
import { assertGreaterThan } from "../src/assert";
import { crearPagina, crearComentario, borrarPagina } from "../src/seed";
import "../src/env";

async function main() {
  const espacio = process.env.ESPACIO || "ComentariosIA";
  const pagina = process.env.PAGINA || "PruebaStagehand";
  const baseUrl = process.env.BASE_URL || "http://localhost:8080";
  const semilla = `Comentario semilla ${Date.now()}`;

  // Arrange (API): pagina minima + 1 comentario => arbol del DOM pequeno
  await crearPagina(espacio, pagina);
  await crearComentario(espacio, pagina, semilla);

  const sh = createStagehand();
  await sh.init();
  const page = sh.context.activePage()!;

  try {
    await loginConIA(sh);

    await page.goto(`${baseUrl}/bin/view/${espacio}/${pagina}?viewer=comments`);
    await page.waitForLoadState("load");
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    await page.waitForTimeout(800);

    // OBSERVE (best-effort: algunos modelos fallan el formato interno del elementId)
    try {
      const acciones = await sh.observe("seccion de comentarios de la pagina con sus mensajes");
      console.log(`observe → ${acciones.length} elementos detectados`);
    } catch (e) {
      console.log("observe no disponible con este modelo, se continua con extract.");
    }

    // EXTRACT: verificacion real con IA
    const data = await sh.extract(
      "extrae los comentarios visibles con autor y mensaje",
      z.object({
        comentarios: z.array(z.object({
          autor: z.string().nullable(),
          mensaje: z.string(),
        })),
      }),
    );
    console.log(`${data.comentarios.length} comentarios extraidos:`, data.comentarios);
    assertGreaterThan(data.comentarios.length, 0, "No se extrajo ningun comentario");

    console.log("✅ ver-comentario PASSED");
  } finally {
    await sh.close();
    await borrarPagina(espacio, pagina);
  }
}

main().catch((e) => { console.error("❌ FAILED:", e); process.exit(1); });
