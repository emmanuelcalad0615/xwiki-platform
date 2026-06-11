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

// Login determinista (Playwright, sin IA): ahorra llamadas al LLM para no agotar
// el limite de tokens por minuto de Groq. Se usa cuando lo que se demuestra con
// IA es la accion/verificacion sobre los comentarios, no el login.
export async function loginDeterminista(sh: Stagehand) {
  const baseUrl = process.env.BASE_URL || "http://localhost:8080";
  const usuario = process.env.XWIKI_USER!;
  const clave = process.env.XWIKI_PASS!;

  const page = sh.context.activePage()!;
  await page.goto(`${baseUrl}/bin/login/XWiki/XWikiLogin`);
  await page.waitForLoadState("load");
  // El page de Stagehand v3 no expone fill/click con selector; se usa evaluate.
  await page.evaluate(
    ({ u, p }: { u: string; p: string }) => {
      const user = document.querySelector("#j_username") as HTMLInputElement | null;
      const pass = document.querySelector("#j_password") as HTMLInputElement | null;
      if (user) user.value = u;
      if (pass) pass.value = p;
      const form = (user?.closest("form") || document.querySelector("form")) as HTMLFormElement | null;
      form?.submit();
    },
    { u: usuario, p: clave },
  );
  await waitForURL(page, /\/bin\/view\//, 15000);
}
