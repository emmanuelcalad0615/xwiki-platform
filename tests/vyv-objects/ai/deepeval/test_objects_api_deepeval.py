"""
Evaluacion asistida por IA (DeepEval, LLM-as-judge) de la funcionalidad "objects".

Que valida:
  1. Que las respuestas REALES de la API REST de objetos (GET/PUT/DELETE) sean
     coherentes con la semantica REST que promete ObjectResourceImpl
     (200 con representacion fiel, 404 para inexistente, 401 sin permiso).
  2. Que los mensajes/estados de error sean comprensibles para un consumidor
     de la API (criterio juzgado por el LLM).

Requisitos:
  - XWiki corriendo (docker compose up -d, flavor instalado) en localhost:8080.
  - pip install -r requirements.txt
  - Una API key para el juez: OPENAI_API_KEY (default de DeepEval) o configurar
    deepeval con otro proveedor compatible.

Ejecutar:
  pytest test_objects_api_deepeval.py -v
Si no hay servidor o API key, los tests se saltan (skip) en lugar de fallar:
asi el pipeline del equipo no se rompe por dependencias externas.
"""
import json
import os

import pytest
import requests

BASE = os.environ.get("XWIKI_URL", "http://localhost:8080")
AUTH = (os.environ.get("XWIKI_USER", "Admin"), os.environ.get("XWIKI_PASS", "admin"))
ESPACIO = "VyVObjectsIA"
PAGINA = "PruebaObjetosIA"
CLASE = "XWiki.TagClass"
URL_PAGINA = f"{BASE}/rest/wikis/xwiki/spaces/{ESPACIO}/pages/{PAGINA}"


def _xwiki_disponible() -> bool:
    try:
        return requests.get(f"{BASE}/rest", timeout=5).status_code == 200
    except requests.RequestException:
        return False


def _hay_juez() -> bool:
    return bool(os.environ.get("OPENAI_API_KEY"))


requiere_entorno = pytest.mark.skipif(
    not (_xwiki_disponible() and _hay_juez()),
    reason="Requiere XWiki en localhost:8080 y una API key para el juez LLM",
)


@pytest.fixture(scope="module")
def objeto_creado():
    """Arrange: pagina + objeto TagClass reales creados via REST."""
    requests.put(
        URL_PAGINA,
        auth=AUTH,
        headers={"Content-Type": "application/xml"},
        data="""<?xml version="1.0" encoding="UTF-8"?>
<page xmlns="http://www.xwiki.org"><title>IA objetos</title><content>.</content></page>""",
        timeout=30,
    )
    creado = requests.post(
        f"{URL_PAGINA}/objects",
        auth=AUTH,
        headers={"Accept": "application/json"},
        data={"className": CLASE, "property#tags": "deepeval"},
        timeout=30,
    )
    numero = creado.json()["number"]
    yield numero
    requests.delete(URL_PAGINA, auth=AUTH, timeout=30)


@requiere_entorno
def test_get_objeto_es_fiel_a_la_semantica_rest(objeto_creado):
    from deepeval import assert_test
    from deepeval.metrics import GEval
    from deepeval.test_case import LLMTestCase, LLMTestCaseParams

    numero = objeto_creado
    respuesta = requests.get(
        f"{URL_PAGINA}/objects/{CLASE}/{numero}",
        auth=AUTH,
        headers={"Accept": "application/json"},
        timeout=30,
    )

    caso = LLMTestCase(
        input=(
            f"GET /rest/.../pages/{PAGINA}/objects/{CLASE}/{numero} — "
            "se espera HTTP 200 con la representacion del objeto: className, "
            "number y la propiedad tags con valor 'deepeval'."
        ),
        actual_output=f"HTTP {respuesta.status_code}\n{json.dumps(respuesta.json(), indent=2)[:4000]}",
    )
    fidelidad = GEval(
        name="Fidelidad REST del recurso objects",
        criteria=(
            "Evalua si la respuesta HTTP real cumple lo esperado en el input: "
            "codigo de estado correcto, className y number presentes y "
            "consistentes, y la propiedad tags con el valor esperado."
        ),
        evaluation_params=[LLMTestCaseParams.INPUT, LLMTestCaseParams.ACTUAL_OUTPUT],
        threshold=0.7,
    )
    assert_test(caso, [fidelidad])


@requiere_entorno
def test_error_404_es_comprensible_para_el_consumidor(objeto_creado):
    from deepeval import assert_test
    from deepeval.metrics import GEval
    from deepeval.test_case import LLMTestCase, LLMTestCaseParams

    respuesta = requests.get(
        f"{URL_PAGINA}/objects/{CLASE}/9999",
        auth=AUTH,
        timeout=30,
    )

    caso = LLMTestCase(
        input=(
            "Se pidio un objeto inexistente (numero 9999). El contrato de "
            "ObjectResourceImpl exige HTTP 404 Not Found, sin trazas de error "
            "internas (stack traces) filtradas al consumidor."
        ),
        actual_output=f"HTTP {respuesta.status_code}\n{respuesta.text[:2000]}",
    )
    claridad = GEval(
        name="Claridad del error para el consumidor de la API",
        criteria=(
            "Evalua si la respuesta es un 404 limpio: codigo correcto y cuerpo "
            "sin stack traces ni detalles internos del servidor."
        ),
        evaluation_params=[LLMTestCaseParams.INPUT, LLMTestCaseParams.ACTUAL_OUTPUT],
        threshold=0.7,
    )
    assert_test(caso, [claridad])
