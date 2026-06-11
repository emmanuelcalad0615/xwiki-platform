import type { Stagehand } from "@browserbasehq/stagehand";
import { waitForURL } from "./utils";
import "./env";

export async function loginConIA(sh: Stagehand) {
  const baseUrl = process.env.BASE_URL || "http://localhost:8080";
  const usuario = process.env.XWIKI_USER!;
  const clave = process.env.XWIKI_PASS!;

  const page = sh.context.activePage()!;
  await page.goto(`${baseUrl}/bin/login/XWiki/XWikiLogin`);
  await page.waitForLoadState("load");

  await sh.act(`escribe "${usuario}" en el campo de nombre de usuario`);
  await sh.act(`escribe "${clave}" en el campo de contraseña`);
  await sh.act("haz click en el botón de iniciar sesión");

  // Tras el login XWiki redirige a una vista /bin/view/... (la pagina de login es /bin/login)
  await waitForURL(page, /\/bin\/view\//, 15000);
}
