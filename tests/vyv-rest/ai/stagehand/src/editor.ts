// Inserta texto en el editor CKEditor de comentarios de XWiki de forma
// determinista. La IA (sh.act "type") NO logra escribir en editores rich-text
// (contenteditable en iframe), asi que aqui usamos la API JS de CKEditor.
export async function escribirEnEditorComentario(page: any, texto: string) {
  // Dar tiempo a que CKEditor termine de cargar tras abrir el formulario
  await page.waitForTimeout(2000);

  const ok = await page.evaluate((t: string) => {
    const w = window as any;
    // CKEditor 4 (el que usa XWiki para comentarios): instancia global
    if (w.CKEDITOR && w.CKEDITOR.instances) {
      const inst: any = Object.values(w.CKEDITOR.instances)[0];
      if (inst) {
        inst.setData(t);
        if (typeof inst.updateElement === "function") inst.updateElement(); // sincroniza al textarea
        return true;
      }
    }
    return false;
  }, texto);

  if (!ok) {
    // Fallback: escribir directo en el iframe del editor con Playwright nativo
    const frame = page
      .frameLocator("iframe.cke_wysiwyg_frame, iframe[title*='Rich Text'], iframe[title*='Editor']")
      .first();
    await frame.locator("body").click({ force: true }).catch(() => {});
    await frame.locator("body").type(texto).catch(() => {});
    // sincronizar por si el textarea no se actualizo
    await page.evaluate(() => {
      const w = window as any;
      if (w.CKEDITOR && w.CKEDITOR.instances) {
        const inst: any = Object.values(w.CKEDITOR.instances)[0];
        if (inst && typeof inst.updateElement === "function") inst.updateElement();
      }
    }).catch(() => {});
  }
}
