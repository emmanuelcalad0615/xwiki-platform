import { z } from "zod";
import { createStagehand } from "../src";
import { loginConIA } from "../src/login";
import { assertEquals, assertGreaterThan } from "../src/assert";
import { crearPagina, borrarPagina } from "../src/seed";
import "../src/env";

async function main() {
  const espacio = process.env.ESPACIO || "ComentariosIA";
  const pagina = process.env.PAGINA || "PruebaStagehand";
  const baseUrl = process.env.BASE_URL || "http://localhost:8080";

  // Arrange (API): pagina minima dedicada => arbol del DOM pequeno
  await crearPagina(espacio, pagina);

  const sh = createStagehand();
  await sh.init();
  const page = sh.context.activePage()!;

  try {
    await loginConIA(sh);

    await page.goto(`${baseUrl}/bin/view/${espacio}/${pagina}?viewer=comments`);
    await page.waitForLoadState("load");
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    await page.waitForTimeout(800);

    const texto = `Comentario IA ${Date.now()}`;

    // ACT: crear el comentario que luego se borrara
    await sh.act("haz click en el boton o enlace para agregar un comentario");
    await sh.act(`escribe "${texto}" en el campo de texto del comentario`);
    await sh.act("haz click en el boton para publicar el comentario");
    await page.waitForTimeout(2500);

    // EXTRACT: contar antes
    const antes = await sh.extract(
      `cuenta cuantos comentarios contienen exactamente el texto "${texto}"`,
      z.object({ cantidad: z.number() }),
    );
    console.log("antes:", antes);
    assertGreaterThan(antes.cantidad, 0, "Comentario no se creo");

    // ACT: borrar
    await sh.act(`haz click en el boton de eliminar/borrar del comentario que dice "${texto}"`);
    await page.waitForTimeout(2500);

    // EXTRACT: contar despues
    const despues = await sh.extract(
      `cuenta cuantos comentarios contienen exactamente el texto "${texto}"`,
      z.object({ cantidad: z.number() }),
    );
    console.log("despues:", despues);
    assertEquals(despues.cantidad, 0, "Comentario NO se elimino");

    console.log("✅ eliminar-comentario PASSED");
  } finally {
    await sh.close();
    await borrarPagina(espacio, pagina);
  }
}

main().catch((e) => { console.error("❌ FAILED:", e); process.exit(1); });
