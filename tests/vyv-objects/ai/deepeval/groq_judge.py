"""
Juez LLM para DeepEval respaldado por Groq (API compatible con OpenAI).

Permite correr las metricas GEval sin cuenta de OpenAI: basta exportar
GROQ_API_KEY (PowerShell: $env:GROQ_API_KEY = "gsk_..."; NUNCA commitearla).
Modelo por defecto: llama-3.3-70b-versatile (se cambia con GROQ_MODEL).
"""
import os

from deepeval.models.base_model import DeepEvalBaseLLM
from openai import OpenAI

BASE_URL = "https://api.groq.com/openai/v1"


class GroqJudge(DeepEvalBaseLLM):
    """Adaptador DeepEvalBaseLLM -> chat.completions de Groq."""

    def __init__(self):
        self.nombre_modelo = os.environ.get("GROQ_MODEL", "llama-3.3-70b-versatile")
        self.cliente = OpenAI(api_key=os.environ["GROQ_API_KEY"], base_url=BASE_URL)

    def load_model(self):
        return self.cliente

    def generate(self, prompt: str, schema=None) -> str:
        respuesta = self.cliente.chat.completions.create(
            model=self.nombre_modelo,
            temperature=0,
            messages=[{"role": "user", "content": prompt}],
        )
        texto = respuesta.choices[0].message.content
        if schema is not None:
            # DeepEval pide a veces salida estructurada; el prompt ya exige JSON,
            # asi que se parsea al esquema pydantic solicitado.
            import json
            import re

            bloque = re.search(r"\{.*\}", texto, re.DOTALL)
            return schema.model_validate(json.loads(bloque.group(0) if bloque else texto))
        return texto

    async def a_generate(self, prompt: str, schema=None) -> str:
        return self.generate(prompt, schema)

    def get_model_name(self) -> str:
        return f"Groq {self.nombre_modelo}"
