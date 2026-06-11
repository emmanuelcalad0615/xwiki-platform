import { z } from "zod";
import { createStagehand } from "../src";
import { loginDeterminista } from "../src/login";
import { assertEquals } from "../src/assert";
import { crearPagina, crearComentario, borrarPagina } from "../src/seed";
import { extraerConReintento } from "../src/extract";
import "../src/env";

// El comentario se crea por API REST (semilla confiable) y la IA lo BORRA desde
// la interfaz (act = click, que la IA si maneja bien) y verifica con extract que
// el comentario desaparecio.
async function main() {
  const espacio = process.env.ESPACIO || "ComentariosIA";
  const pagina = process.env.PAGINA || "PruebaStagehand";
  const baseUrl = process.env.BASE_URL || "http://localhost:8080";
  const texto = `Comentario IA ${Date.now()}`;

  // Arrange (API): pagina minima + el comentario que la IA borrara
  await crearPagina(espacio, pagina);
  await crearComentario(espacio, pagina, texto);

  const sh = createStagehand();
  await sh.init();
  const page = sh.context.activePage()!;

  try {
    await loginDeterminista(sh);

    await page.goto(`${baseUrl}/bin/view/${espacio}/${pagina}?viewer=comments`);
    await page.waitForLoadState("load");
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    await page.waitForTimeout(800);

    // (El comentario semilla ya existe: se creo por API arriba.)

    // ACT (IA): borrar el comentario desde la UI
    await sh.act(`haz click en el icono o boton de eliminar/borrar del comentario que dice "${texto}"`);
    await page.waitForTimeout(1500);
    // Por si XWiki muestra un dialogo de confirmacion (best-effort)
    try {
      await sh.act("haz click en el boton de confirmar o aceptar el borrado");
    } catch (e) {
      // no habia dialogo de confirmacion
    }
    await page.waitForTimeout(1500);

    // Recargar la vista para reflejar el borrado
    await page.goto(`${baseUrl}/bin/view/${espacio}/${pagina}?viewer=comments`);
    await page.waitForLoadState("load");
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    await page.waitForTimeout(800);

    // EXTRACT (IA): contar despues (debe ser 0) — con reintento por el bug intermitente
    const despues = await extraerConReintento(
      sh,
      `cuenta cuantos comentarios contienen exactamente el texto "${texto}"`,
      z.object({ cantidad: z.number() }),
    );
    console.log("despues:", despues);
    assertEquals(despues.cantidad, 0, "El comentario NO se elimino");

    console.log("✅ eliminar-comentario PASSED");
  } finally {
    await sh.close();
    await borrarPagina(espacio, pagina);
  }
}

main().catch((e) => { console.error("❌ FAILED:", e); process.exit(1); });
