import * as fs from "node:fs";
import * as path from "node:path";

export interface EvalEntry {
  id: string;
  primitive: "act" | "observe" | "extract";
  input: string;
  actual_output: string;
  expected_output: string;
  context: string[];
  metadata?: Record<string, unknown>;
}

export class EvalRecorder {
  private entries: EvalEntry[] = [];

  add(entry: EvalEntry) {
    this.entries.push({
      ...entry,
      metadata: { ...(entry.metadata ?? {}), timestamp: new Date().toISOString() },
    });
  }

  async save(filePath: string) {
    const fullPath = path.resolve(filePath);
    const dir = path.dirname(fullPath);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(
      fullPath,
      JSON.stringify({ tests: this.entries }, null, 2),
      "utf-8",
    );
    console.log(`[eval-recorder] guardado ${this.entries.length} casos en ${fullPath}`);
  }
}

export function truncar(texto: string, max = 8000): string {
  if (texto.length <= max) return texto;
  return texto.slice(0, max) + `\n... [truncado, total ${texto.length} chars]`;
}
