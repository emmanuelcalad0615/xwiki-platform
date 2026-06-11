"""
Ollama local como modelo JUEZ de DeepEval.

Sin rate limits, gratis, cumple rúbrica ("Gemini o Ollama local").
Requiere Ollama corriendo en localhost:11434 y modelo descargado.

    ollama pull llama3.2

Uso del modo JSON nativo de Ollama (format="json") + validación Pydantic.
"""
import json
import os
from typing import Optional, Type
from pydantic import BaseModel

from ollama import Client
from deepeval.models import DeepEvalBaseLLM
from dotenv import load_dotenv

load_dotenv()

MODEL_NAME = os.getenv("OLLAMA_MODEL", "llama3.2")
OLLAMA_HOST = os.getenv("OLLAMA_HOST", "http://localhost:11434")


def _build_example_from_schema(schema: Type[BaseModel]) -> dict:
    """Genera un ejemplo concreto a partir de los campos del modelo Pydantic."""
    example = {}
    for name, field in schema.model_fields.items():
        ann = field.annotation
        ann_str = str(ann).lower()
        if "list" in ann_str or "List" in str(ann):
            example[name] = ["ejemplo1", "ejemplo2"]
        elif "int" in ann_str or "float" in ann_str:
            example[name] = 0
        elif "bool" in ann_str:
            example[name] = True
        elif "dict" in ann_str:
            example[name] = {}
        else:
            example[name] = "texto de ejemplo"
    return example


def _build_schema_prompt(prompt: str, schema: Type[BaseModel]) -> str:
    json_schema = schema.model_json_schema()
    field_names = list(schema.model_fields.keys())
    example = _build_example_from_schema(schema)
    return (
        f"{prompt}\n\n"
        f"=== INSTRUCCIONES CRÍTICAS DE FORMATO ===\n"
        f"Debes responder con un OBJETO JSON que tenga EXACTAMENTE estos campos "
        f"en el nivel raíz: {field_names}.\n\n"
        f"EJEMPLO de la FORMA EXACTA que debe tener tu respuesta "
        f"(con valores reales, NO copies estos placeholders):\n"
        f"{json.dumps(example, indent=2, ensure_ascii=False)}\n\n"
        f"Schema de referencia (NO LO COPIES, solo úsalo para saber los tipos):\n"
        f"{json.dumps(json_schema, indent=2, ensure_ascii=False)}\n\n"
        f"REGLAS:\n"
        f"- NO copies el schema en tu respuesta.\n"
        f"- NO uses las palabras 'properties', 'type', 'required' como llaves raíz.\n"
        f"- Las llaves del JSON raíz deben ser EXACTAMENTE: {field_names}.\n"
        f"- Solo el JSON, sin explicaciones, sin markdown."
    )


class OllamaJudge(DeepEvalBaseLLM):
    def __init__(self, model_name: str = MODEL_NAME, host: str = OLLAMA_HOST):
        self._model_name = model_name
        self._client = Client(host=host)

    def load_model(self):
        return self._client

    def generate(self, prompt: str, schema: Optional[Type[BaseModel]] = None):
        if schema is not None:
            full_prompt = _build_schema_prompt(prompt, schema)
            resp = self._client.chat(
                model=self._model_name,
                messages=[{"role": "user", "content": full_prompt}],
                format="json",
            )
            return schema.model_validate_json(resp["message"]["content"])
        resp = self._client.chat(
            model=self._model_name,
            messages=[{"role": "user", "content": prompt}],
        )
        return resp["message"]["content"]

    async def a_generate(self, prompt: str, schema: Optional[Type[BaseModel]] = None):
        return self.generate(prompt, schema)

    def get_model_name(self) -> str:
        return self._model_name
