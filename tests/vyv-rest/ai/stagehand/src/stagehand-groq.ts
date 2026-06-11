import { Stagehand } from "@browserbasehq/stagehand";
import "./env";

export function createStagehandGroq() {
  if (!process.env.GROQ_API_KEY || process.env.GROQ_API_KEY.startsWith("gsk_PEGA")) {
    throw new Error("Falta GROQ_API_KEY en .env");
  }
  return new Stagehand({
    env: "LOCAL",
    model: {
      modelName: `groq/${process.env.GROQ_MODEL || "llama-3.3-70b-versatile"}`,
      apiKey: process.env.GROQ_API_KEY,
    },
    localBrowserLaunchOptions: {
      headless: process.env.HEADLESS === "true",
      args: [
        "--disable-features=Translate,PasswordManager,AutofillServerCommunication,PasswordLeakDetection",
        "--disable-save-password-bubble",
        "--no-default-browser-check",
        "--disable-blink-features=AutomationControlled",
        "--lang=es-CO",
      ],
    },
    verbose: 1,
    serverCache: true,
  });
}
