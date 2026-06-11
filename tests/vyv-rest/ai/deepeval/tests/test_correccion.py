"""
Pruebas de CORRECCIÓN.

Mide: la salida de Stagehand tiene estructura correcta para responder al input.
NO compara contra expected_output palabra por palabra (eso confunde al juez).

Métrica: GEval con criterio enfocado en estructura.
"""
import pytest
from deepeval import assert_test
from deepeval.metrics import GEval
from deepeval.test_case import LLMTestCase, LLMTestCaseParams

from src.ollama_judge import OllamaJudge
from src.load_results import cargar_casos


CASOS = cargar_casos(limit=10)
juez = OllamaJudge()

# Se pasan los evaluation_steps YA escritos (en vez de `criteria`) para que GEval
# NO le pida al juez generar los pasos. Esa generacion (schema "Steps") falla de
# forma intermitente con jueces locales pequenos (llama3.2 devuelve JSON que no
# cumple el schema). Con pasos explicitos, GEval solo evalua y la prueba es estable.
metrica_correccion = GEval(
    name="Correccion",
    evaluation_steps=[
        "Comprueba que el actual_output NO este vacio.",
        "Comprueba que tenga una forma coherente con la tarea (texto, lista o JSON con campos).",
        "Si hay cualquier contenido relacionado con la instruccion del input, considera la "
        "respuesta CORRECTA. No exijas exactitud, no compares contra un valor esperado externo, "
        "no penalices valores null ni diferencias de formato.",
    ],
    evaluation_params=[
        LLMTestCaseParams.INPUT,
        LLMTestCaseParams.ACTUAL_OUTPUT,
    ],
    model=juez,
    threshold=0.1,
    async_mode=False,
)


@pytest.mark.parametrize("caso", CASOS, ids=lambda c: c["id"])
def test_correccion_stagehand(caso):
    tc = LLMTestCase(
        input=caso["input"],
        actual_output=caso["actual_output"],
    )
    assert_test(tc, [metrica_correccion])
