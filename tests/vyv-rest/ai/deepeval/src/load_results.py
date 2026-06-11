"""Lee el JSON que Stagehand TS genera con sus outputs de IA.

Post-procesa contextos recortando a la sección relevante por caso (quita header,
nav y footer repetidos que confunden al juez local con contextos largos).
Los actual_output se entregan crudos como los devolvió Stagehand.
"""
import json
import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv()

RESULTS_PATH = os.getenv("STAGEHAND_RESULTS_PATH", "./data/stagehand-results.json")
MAX_CTX_CHARS = 8000
MAX_OUT_CHARS = 3000
MAX_EXP_CHARS = 1500


def _truncar(s: str, n: int) -> str:
    if not isinstance(s, str):
        return s
    return s if len(s) <= n else s[:n] + " ...[truncado]"


def _recortar_contexto(case_id: str, ctx: str) -> str:
    """Recorta contexto a la sección de comentarios. Quita header/nav/footer
    repetidos que confunden al juez con contextos largos."""
    if not isinstance(ctx, str):
        return ctx
    if case_id in ("extract-comentarios", "observe-comentarios", "act-agregar-comentario"):
        # XWiki muestra los comentarios bajo el encabezado "Comments"/"Comentarios"
        for marca in ("Comentarios", "Comments"):
            i = ctx.find(marca)
            if i != -1:
                seccion = ctx[i:]
                # Cortar el pie de pagina de XWiki si aparece
                for fin in ("Powered by", "Privacy", "Información", "Information"):
                    j = seccion.find(fin)
                    if j != -1:
                        seccion = seccion[:j]
                        break
                return seccion.strip()
    return ctx


def cargar_casos(limit: int | None = None):
    """Devuelve lista de casos desde el JSON generado por Stagehand.
    Aplica recorte de contexto, truncado y limit."""
    path = Path(RESULTS_PATH)
    if not path.exists():
        raise RuntimeError(
            f"No existe {path.resolve()}.\n"
            "Primero corre el flujo Stagehand de captura:\n"
            "  cd ../stagehand\n"
            "  npm run stagehand:eval"
        )
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    casos = data.get("tests", [])
    if limit is not None:
        casos = casos[:limit]

    for c in casos:
        c["actual_output"] = _truncar(c.get("actual_output", ""), MAX_OUT_CHARS)
        c["expected_output"] = _truncar(c.get("expected_output", ""), MAX_EXP_CHARS)
        ctx = c.get("context", [])
        cid = c.get("id", "")
        ctx = [_recortar_contexto(cid, x) for x in ctx]
        c["context"] = [_truncar(x, MAX_CTX_CHARS) for x in ctx]
    return casos
