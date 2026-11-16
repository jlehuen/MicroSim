"""Optional LLM-assisted pedagogical feedback for failed or partial TP4 submissions.

Uses only Python stdlib (urllib) and supports local Ollama or OpenAI-compatible endpoints.
"""

from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request


DEFAULT_OLLAMA_MODEL = "qwen2.5-coder:7b"


def generate_feedback(
    *,
    exercise_title: str,
    source_code: str,
    assembly_error: str | None,
    notes: list[str],
    model: str | None = None,
    timeout: float = 60.0,
) -> str | None:
    """Return a concise French pedagogical hint, or None if the LLM endpoint is unavailable."""
    prompt = (
        f"Tu es un enseignant d'architecture des ordinateurs (assembleur x86 8 bits MicroSim : "
        f"registres AL/BL/CL/DL, ports 0x01 clavier, 0x02 feux, 0x03 chauffage, terminal 0xC0-0xFF, "
        f"fin par hlt, nombres hexa avec 0x).\n"
        f"Exercice : {exercise_title}\n"
        f"Erreur d'assemblage : {assembly_error or 'aucune'}\n"
        f"Constat d'évaluation : {'; '.join(notes)}\n"
        f"Code de l'étudiant :\n```asm\n{source_code.strip()}\n```\n"
        f"Donne un retour pédagogique très court (2 phrases max en français) : "
        f"souligne ce qui est juste dans la démarche, puis indique précisément ce qu'il faut corriger."
    )

    api_key = os.environ.get("OPENAI_API_KEY")
    use_openai = bool(api_key) and os.environ.get("LLM_PROVIDER", "").lower() == "openai"
    if use_openai:
        base_url = os.environ.get("OPENAI_BASE_URL", "https://api.openai.com/v1").rstrip("/")
        chosen_model = model or os.environ.get("OPENAI_MODEL", "gpt-4o-mini")
        payload = {
            "model": chosen_model,
            "messages": [{"role": "user", "content": prompt}],
            "temperature": 0.2,
            "max_tokens": 160,
        }
        req = urllib.request.Request(
            f"{base_url}/chat/completions",
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {api_key}",
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                return data["choices"][0]["message"]["content"].strip()
        except (urllib.error.URLError, KeyError, IndexError, TimeoutError, json.JSONDecodeError, OSError) as exc:
            print(
                f"[AVERTISSEMENT IA] Échec de génération pour {exercise_title} ({chosen_model}) : {exc}",
                file=sys.stderr,
            )
            return None

    ollama_host = os.environ.get("OLLAMA_HOST", "http://127.0.0.1:11434").rstrip("/")
    chosen_model = model or os.environ.get("OLLAMA_MODEL", DEFAULT_OLLAMA_MODEL)
    payload = {
        "model": chosen_model,
        "prompt": prompt,
        "stream": False,
        "options": {"temperature": 0.2, "num_predict": 160},
    }
    req = urllib.request.Request(
        f"{ollama_host}/api/generate",
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        if "127.0.0.1" in ollama_host or "localhost" in ollama_host:
            opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
            resp_ctx = opener.open(req, timeout=timeout)
        else:
            resp_ctx = urllib.request.urlopen(req, timeout=timeout)
        with resp_ctx as resp:
            data = json.loads(resp.read().decode("utf-8"))
            text = (data.get("response") or "").strip()
            return text or None
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, OSError) as exc:
        print(
            f"[AVERTISSEMENT IA] Échec de génération pour {exercise_title} ({chosen_model} sur {ollama_host}) : {exc}",
            file=sys.stderr,
        )
        return None
