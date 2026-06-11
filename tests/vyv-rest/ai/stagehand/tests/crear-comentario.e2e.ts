import { z } from "zod";
import { createStagehand } from "../src";
import { loginConIA } from "../src/login";
import { assertGreaterThan } from "../src/assert";
import { crearPagina, crearComentario, borrarPagina } from "../src/seed";
import "../src/env";

// El comentario se crea por API REST (el editor CKEditor de XWiki no acepta de
// forma confiable el typing por IA) y la IA VERIFICA con extract que el
// comentario creado aparece en la interfaz.
async function main() {
  const espacio = process.env.ESPACIO || "ComentariosIA";
  const pagina = process.env.PAGINA || "PruebaStagehand";
  const baseUrl = process.env.BASE_URL || "http://localhost:8080";
  const texto = `Comentario IA ${Date.now()}`;

  // Arrange (API): pagina minima + el comentario a verificar
  await crearPagina(espacio, pagina);
  await crearComentario(espacio, pagina, texto);

  const sh = createStagehand();
  await sh.init();
  const page = sh.context.activePage()!;

  try {
    await loginConIA(sh);

    await page.goto(`${baseUrl}/bin/view/${espacio}/${pagina}?viewer=comments`);
    await page.waitForLoadState("load");
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    await page.waitForTimeout(800);

    // EXTRACT (IA): verifica que el comentario creado aparece en la UI
    const data = await sh.extract(
      `cuenta cuantos comentarios contienen exactamente el texto "${texto}"`,
      z.object({ cantidad: z.number() }),
    );
    console.log("comentarios con el texto:", data);
    assertGreaterThan(data.cantidad, 0, "El comentario creado no aparece en la UI");

    console.log("✅ crear-comentario PASSED");
  } finally {
    await sh.close();
    await borrarPagina(espacio, pagina);
  }
}

main().catch((e) => { console.error("❌ FAILED:", e); process.exit(1); });
