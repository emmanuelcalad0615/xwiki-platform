import "./env";

// Helpers de datos via API REST: crean una pagina minima y dedicada para las
// pruebas de IA (arbol del DOM pequeno, evita el limite de tokens del LLM) y
// la limpian al terminar.
const BASE = process.env.BASE_URL || "http://localhost:8080";
const USER = process.env.XWIKI_USER!;
const PASS = process.env.XWIKI_PASS!;
const auth = "Basic " + Buffer.from(`${USER}:${PASS}`).toString("base64");

export async function crearPagina(espacio: string, pagina: string) {
  await fetch(`${BASE}/rest/wikis/xwiki/spaces/${espacio}/pages/${pagina}`, {
    method: "PUT",
    headers: { Authorization: auth, "Content-Type": "application/xml" },
    body:
      `<?xml version="1.0" encoding="UTF-8"?>` +
      `<page xmlns="http://www.xwiki.org"><title>Prueba Stagehand Comentarios</title>` +
      `<content>Pagina minima para pruebas de IA.</content></page>`,
  });
}

export async function crearComentario(espacio: string, pagina: string, texto: string) {
  const r = await fetch(`${BASE}/rest/wikis/xwiki/spaces/${espacio}/pages/${pagina}/comments`, {
    method: "POST",
    headers: { Authorization: auth, "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({ text: texto }),
  });
  if (r.status !== 201) throw new Error(`No se pudo crear comentario via API (HTTP ${r.status})`);
}

export async function borrarPagina(espacio: string, pagina: string) {
  await fetch(`${BASE}/rest/wikis/xwiki/spaces/${espacio}/pages/${pagina}`, {
    method: "DELETE",
    headers: { Authorization: auth },
  }).catch(() => {});
}
