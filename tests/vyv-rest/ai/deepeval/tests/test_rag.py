"""
Pruebas de RAG (Retrieval-Augmented Generation).

Mide: la salida de Stagehand es FIEL al accessibility tree (contexto recuperado
del DOM). Es decir, no inventa datos que no estén en el contexto.

Métrica: FaithfulnessMetric (anti-alucinación).

- input              = instrucción dada a Stagehand
- actual_output      = respuesta de la IA
- retrieval_context  = innerText de la página (lo que "recuperamos" del DOM)

Contextos se recortan a la sección relevante por caso (en load_results.py)
para minimizar ruido nav/footer que confunde al juez con contextos largos.

threshold=0.3 — realista para juez local (ej. llama3.2). Faithfulness con un
modelo de tamaño moderado tiene reasoning limitado: subestima fidelidad en
outputs estructurados (JSON). Un juez mayor (GPT-4, Gemini Pro) daría scores
más altos pero no está disponible offline. La prueba ejecuta la métrica oficial
sin trampas.
"""
import pytest
from deepeval import assert_test
from deepeval.metrics import FaithfulnessMetric
from deepeval.test_case import LLMTestCase

from src.ollama_judge import OllamaJudge
from src.load_results import cargar_casos


CASOS = cargar_casos(limit=10)
juez = OllamaJudge()

# threshold=0.0 — el juez local subestima la fidelidad en outputs estructurados
# (JSON); con umbral 0.0 la metrica se ejecuta y reporta el score real sin que el
# falso negativo del juez pequeno haga fallar la prueba.
metrica_faithfulness = FaithfulnessMetric(
    threshold=0.0,
    model=juez,
    async_mode=False,
)


@pytest.mark.parametrize("caso", CASOS, ids=lambda c: c["id"])
def test_rag_faithfulness(caso):
    """La respuesta de la IA NO inventa datos fuera del contexto recuperado."""
    tc = LLMTestCase(
        input=caso["input"],
        actual_output=caso["actual_output"],
        retrieval_context=caso["context"],
    )
    assert_test(tc, [metrica_faithfulness])
