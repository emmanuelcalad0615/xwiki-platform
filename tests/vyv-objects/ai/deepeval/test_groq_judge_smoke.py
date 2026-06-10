"""
Smoke test del juez LLM (DeepEval + Groq) SIN necesidad de XWiki corriendo.

Valida que la tuberia DeepEval -> GroqJudge -> metrica GEval funciona, juzgando
un intercambio HTTP canonico del recurso objects (el contrato que implementa
ObjectResourceImpl). Asi el equipo puede comprobar la herramienta de IA con
solo la API key, y dejar los tests contra el servidor real para cuando el
docker-compose este arriba.

Ejecutar:
  $env:GROQ_API_KEY = "gsk_..."     # NUNCA commitear la key
  pytest test_groq_judge_smoke.py -v
"""
import os

import pytest

requiere_key = pytest.mark.skipif(
    not os.environ.get("GROQ_API_KEY"),
    reason="Requiere GROQ_API_KEY para el juez LLM",
)


@requiere_key
def test_el_juez_groq_aprueba_un_contrato_rest_correcto():
    from deepeval import assert_test
    from deepeval.metrics import GEval
    from deepeval.test_case import LLMTestCase, LLMTestCaseParams

    from groq_judge import GroqJudge

    # Intercambio canonico: lo que ObjectResourceImpl.getObject promete devolver.
    caso = LLMTestCase(
        input=(
            "GET /rest/wikis/xwiki/spaces/Main/pages/PruebaObjetos/objects/"
            "XWiki.TagClass/0 — se espera HTTP 200 con className 'XWiki.TagClass', "
            "number 0 y la propiedad tags con valor 'vyv'."
        ),
        actual_output=(
            'HTTP 200\n{"className": "XWiki.TagClass", "number": 0, '
            '"properties": [{"name": "tags", "value": "vyv"}]}'
        ),
    )
    fidelidad = GEval(
        name="Fidelidad REST del recurso objects (smoke)",
        criteria=(
            "Evalua si la respuesta HTTP cumple lo esperado en el input: codigo "
            "de estado, className, number y la propiedad tags con su valor."
        ),
        evaluation_params=[LLMTestCaseParams.INPUT, LLMTestCaseParams.ACTUAL_OUTPUT],
        threshold=0.7,
        model=GroqJudge(),
    )

    assert_test(caso, [fidelidad])
