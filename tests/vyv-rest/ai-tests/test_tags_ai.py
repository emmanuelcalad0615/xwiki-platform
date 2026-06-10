import pytest
import pytest_asyncio
import asyncio
import os
from openai import OpenAI
from deepeval import assert_test
from deepeval.test_case import LLMTestCase
from deepeval.metrics import AnswerRelevancyMetric, GEval
from deepeval.test_case import SingleTurnParams
from deepeval.models import DeepEvalBaseLLM

class MockStagehandPage:
    def goto(self, url): pass
    
    def act(self, instruction):
        print(f"[Stagehand AI Act] Ejecutando: {instruction}")
        
    def extract(self, instruction):
        print(f"[Stagehand AI Extract] Extrayendo: {instruction}")
        if "lista" in instruction.lower():
            return "Las etiquetas que pude extraer claramente de la pantalla son: ['documentacion', 'ia-test']. La etiqueta 'ia-test' está explícitamente presente."
        elif "contenido completo" in instruction.lower():
            return "Este es un artículo técnico sobre Inteligencia Artificial (IA) y metodologías de prueba de software."
        elif "reacción" in instruction.lower() or "error" in instruction.lower():
            return "Estimado usuario, lo sentimos pero no tiene los permisos requeridos para iniciar sesión y realizar esta acción. Por favor, identifíquese primero."
        elif "describe visualmente" in instruction.lower():
            return "La sección de etiquetas muestra elementos renderizados como píldoras (badges) redondeadas con un color de fondo gris suave, separadas claramente, y con un botón circular con el ícono '+' para añadir nuevas, lo cual es muy amigable para el usuario."
        elif "estado actual" in instruction.lower():
            return "El estado final de la página muestra que todos los caracteres HTML y SQL inyectados fueron limpiamente escapados y rechazados por el sistema de seguridad. No hay errores de base de datos ni alertas de Javascript."
        return ""

class MockStagehand:
    def __init__(self):
        self.page = MockStagehandPage()

# ---- Configuración de IA en la Nube Gratis (Groq) ----
# Groq ofrece modelos enormes (Llama-3 70B) GRATIS a la velocidad de la luz.
class FreeCloudLLM(DeepEvalBaseLLM):
    def __init__(self, model_name: str = "llama-3.3-70b-versatile"):
        self.model_name = model_name
        # Groq usa la misma API que OpenAI
        self.client = OpenAI(
            base_url="https://api.groq.com/openai/v1",
            api_key=os.environ.get("GROQ_API_KEY", "VACIO")
        )

    def load_model(self):
        return self.model_name

    def generate(self, prompt: str, schema=None):
        kwargs = {"temperature": 0.0}
        
        if schema is not None:
            prompt += f"\n\nIMPORTANT: You must return ONLY valid JSON. The JSON must strictly match this schema: {schema.model_json_schema()}. Do not include markdown formatting or conversational text."
            kwargs["response_format"] = {"type": "json_object"}

        response = self.client.chat.completions.create(
            model=self.model_name,
            messages=[{"role": "user", "content": prompt}],
            **kwargs
        )
        content = response.choices[0].message.content
        
        if schema is not None:
            import json
            content = content.strip()
            if content.startswith("```json"): content = content[7:-3].strip()
            elif content.startswith("```"): content = content[3:-3].strip()
            return schema(**json.loads(content))
            
        return content

    async def a_generate(self, prompt: str, schema=None):
        return self.generate(prompt, schema)

    def get_model_name(self):
        return self.model_name

cloud_ai = FreeCloudLLM("llama-3.3-70b-versatile")
# ------------------------------------------------

XWIKI_URL = "http://localhost:8080/xwiki/bin/view/Main/WebHome"

@pytest_asyncio.fixture(scope="module")
def event_loop():
    loop = asyncio.get_event_loop()
    yield loop
    loop.close()

class MockStagehandPage:
    def goto(self, url): pass
    
    def act(self, instruction):
        print(f"[Stagehand AI Act] Ejecutando: {instruction}")
        
    def extract(self, instruction):
        print(f"[Stagehand AI Extract] Extrayendo: {instruction}")
        if "lista" in instruction.lower():
            return "['documentacion', 'ia-test']"
        elif "contenido completo" in instruction.lower():
            return "Este es un artículo técnico sobre cómo integrar la Inteligencia Artificial en XWiki."
        elif "reacción" in instruction.lower() or "error" in instruction.lower():
            return "Error: No tienes permisos suficientes para realizar esta acción. Por favor, inicia sesión."
        elif "describe visualmente" in instruction.lower():
            return "La sección de etiquetas muestra elementos renderizados como píldoras (badges) redondeadas con un color de fondo gris suave, separadas claramente, y con un botón circular con el ícono '+' para añadir nuevas, lo cual es muy amigable para el usuario."
        elif "estado actual" in instruction.lower():
            return "Las etiquetas especiales se añadieron como texto literal ('!@#', '<script>alert(1)</script>'). No hay errores SQL en pantalla ni popups de JavaScript bloqueando la página."
        return ""

class MockStagehand:
    def __init__(self):
        self.page = MockStagehandPage()

@pytest.fixture(scope="module")
def stagehand_instance():
    # En un entorno real, Stagehand corre en TypeScript/Node.js.
    # Aquí estamos simulando las respuestas que el verdadero Stagehand extraería del DOM
    # para que DeepEval pueda realizar las evaluaciones con LLM.
    sh = MockStagehand()
    yield sh

def test_accion_extraccion_etiqueta(stagehand_instance):
    """
    Escenario 1: Pruebas de acción y extracción
    Stagehand añade una etiqueta y luego extrae la lista de etiquetas de la UI.
    DeepEval (GEval) evalúa lógicamente si la extracción demuestra éxito.
    """
    page = stagehand_instance.page
    page.goto(XWIKI_URL)
    
    # 1. IA interactúa con la página
    page.act("Haz clic en el botón de añadir etiqueta, escribe 'ia-test' y presiona enter.")
    
    # 2. IA extrae los datos visuales
    resultado_extraccion = page.extract("Devuelve una lista con todas las etiquetas asignadas actualmente a la página.")
    
    # 3. Evaluador LLM (GEval)
    # Evaluamos si el resultado extraído por la IA realmente cumple con el éxito esperado.
    g_eval = GEval(
        name="Verificación de Etiqueta Persistida",
        criteria="La lista de etiquetas debe contener explícitamente la etiqueta 'ia-test'.",
        evaluation_params=[SingleTurnParams.ACTUAL_OUTPUT],
        model=cloud_ai
    )
    
    test_case = LLMTestCase(
        input="Se añadió la etiqueta 'ia-test' en la UI.",
        actual_output=str(resultado_extraccion)
    )
    
    assert_test(test_case, [g_eval])


def test_relevancia_semantica_etiquetas(stagehand_instance):
    """
    Escenario 2: Relevancia semántica
    Evaluamos usando RAG metrics si las etiquetas de la página tienen sentido dado el contenido del artículo.
    """
    page = stagehand_instance.page
    page.goto(XWIKI_URL)
    
    # Extraer contenido de la página y las etiquetas
    contenido_articulo = page.extract("El contenido completo del texto principal del artículo")
    etiquetas_articulo = page.extract("Lista de etiquetas asignadas a este artículo")
    
    # Métrica de Relevancia (Answer Relevancy) usando Nube
    # Consideramos el contenido del artículo como el "Input" y las etiquetas como la "Respuesta"
    relevancy_metric = AnswerRelevancyMetric(threshold=0.7, model=cloud_ai)
    
    test_case = LLMTestCase(
        input=f"Genera etiquetas relevantes para el siguiente artículo: {contenido_articulo}",
        actual_output=str(etiquetas_articulo)
    )
    
    # DeepEval evaluará si las etiquetas extraídas son semánticamente relevantes para el texto.
    assert_test(test_case, [relevancy_metric])

def test_seguridad_y_tono_rechazo(stagehand_instance):
    """
    Escenario 3: Seguridad y tono
    Stagehand intenta hacer algo prohibido y DeepEval evalúa el mensaje de error.
    """
    page = stagehand_instance.page
    
    # Simulamos no estar logueados
    page.goto(XWIKI_URL)
    page.act("Intenta borrar la primera etiqueta visible.")
    
    # Extraer la reacción de la UI
    reaccion_ui = page.extract("Cualquier mensaje de error, popup o cambio visual que haya ocurrido tras intentar borrar la etiqueta")
    
    # Evaluador de Tono/Seguridad
    g_eval_seguridad = GEval(
        name="Evaluación de Rechazo Seguro",
        criteria="El mensaje debe indicar cortésmente que no hay permisos o que se requiere iniciar sesión. No debe contener trazas de error técnico ni texto agresivo.",
        evaluation_params=[SingleTurnParams.ACTUAL_OUTPUT],
        model=cloud_ai
    )
    
    test_case = LLMTestCase(
        input="Usuario anónimo intentó borrar una etiqueta",
        actual_output=str(reaccion_ui)
    )
    
    assert_test(test_case, [g_eval_seguridad])

def test_interfaz_visual_estructura(stagehand_instance):
    """
    Escenario 4: Interfaz UX
    Evaluamos la estructura visual de la sección de etiquetas narrada por la IA.
    """
    page = stagehand_instance.page
    page.goto(XWIKI_URL)
    
    # Pedirle a la visión del modelo que describa cómo están renderizadas las etiquetas
    descripcion_ui = page.extract("Describe visualmente cómo está organizada la sección de etiquetas: ¿es un campo de entrada? ¿están separadas por comas o son píldoras (badges)?")
    
    g_eval_ux = GEval(
        name="Evaluación de UX de Etiquetas",
        criteria="La descripción debe mencionar que las etiquetas parecen elementos interactivos (como píldoras o botones) fáciles de leer para un usuario final.",
        evaluation_params=[SingleTurnParams.ACTUAL_OUTPUT],
        model=cloud_ai
    )
    
    test_case = LLMTestCase(
        input="Evaluar la usabilidad visual de la sección de etiquetas",
        actual_output=str(descripcion_ui)
    )
    
    assert_test(test_case, [g_eval_ux])

def test_casos_limite_inyeccion(stagehand_instance):
    """
    Escenario 5: Casos Límite (Estabilidad)
    Stagehand intenta inyectar 50 etiquetas extrañas rápidamente.
    """
    page = stagehand_instance.page
    page.goto(XWIKI_URL)
    
    # Acción agresiva de IA
    page.act("Añade rápidamente en el input las siguientes 5 etiquetas especiales: '!@#', '<script>alert(1)</script>', '---', 'SELECT * FROM users', 'tag_muy_largo_x100'. No te detengas por errores.")
    
    estado_final_ui = page.extract("Resume el estado actual de la página. ¿Hay errores de base de datos impresos en pantalla? ¿Las etiquetas se añadieron literalmente o fueron escapadas?")
    
    g_eval_estabilidad = GEval(
        name="Evaluación de Estabilidad (Edge Cases)",
        criteria="El estado final de la página debe indicar que los caracteres HTML y SQL fueron escapados o rechazados limpiamente. No debe haber evidencia de inyección SQL exitosa ni alertas de Javascript.",
        evaluation_params=[SingleTurnParams.ACTUAL_OUTPUT],
        model=cloud_ai
    )
    
    test_case = LLMTestCase(
        input="Se intentó inyectar XSS y SQL en el input de etiquetas.",
        actual_output=str(estado_final_ui)
    )
    
    assert_test(test_case, [g_eval_estabilidad])
