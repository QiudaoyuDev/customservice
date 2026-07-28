import json
import os
import tempfile
from pathlib import Path
from typing import Any

from fastapi import FastAPI, File, HTTPException, UploadFile
from paddleocr import PaddleOCR

app = FastAPI(title="Local OCR Adapter", version="0.1.0")
ocr: PaddleOCR | None = None


def make_json_safe(value: Any) -> Any:
    if hasattr(value, "tolist"):
        return value.tolist()
    if isinstance(value, bytes):
        return value.decode("utf-8", errors="replace")
    if isinstance(value, dict):
        return {str(k): make_json_safe(v) for k, v in value.items()}
    if isinstance(value, (list, tuple)):
        return [make_json_safe(v) for v in value]
    return value


def collect_text(value: Any) -> list[str]:
    if isinstance(value, dict):
        texts = value.get("rec_texts") or value.get("texts") or []
        found = [str(item) for item in texts] if isinstance(texts, list) else []
        for item in value.values():
            found.extend(collect_text(item))
        return found
    if isinstance(value, (list, tuple)):
        found: list[str] = []
        for item in value:
            found.extend(collect_text(item))
        return found
    return []


@app.on_event("startup")
def load_ocr() -> None:
    global ocr
    # The generic OCR pipeline is deliberately restricted to text extraction.
    # Hardware fault conclusions remain the responsibility of the application
    # diagnosis flow and never this image adapter.
    ocr = PaddleOCR(
        lang=os.getenv("OCR_LANGUAGE", "en"),
        device=os.getenv("OCR_DEVICE", "cpu"),
        use_doc_orientation_classify=False,
        use_doc_unwarping=False,
        use_textline_orientation=False,
    )


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok" if ocr else "starting"}


@app.post("/v1/ocr")
async def recognize(file: UploadFile = File(...)) -> dict[str, Any]:
    if ocr is None:
        raise HTTPException(status_code=503, detail="OCR model is not ready")
    suffix = Path(file.filename or "upload.png").suffix or ".png"
    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="Empty file")
    if len(content) > 20 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="File exceeds 20 MiB limit")

    with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as tmp:
        tmp.write(content)
        tmp_path = tmp.name
    try:
        raw = []
        for result in ocr.predict(tmp_path):
            if hasattr(result, "json"):
                serialized = result.json() if callable(result.json) else result.json
                raw.append(json.loads(serialized))
            elif hasattr(result, "to_json"):
                serialized = result.to_json() if callable(result.to_json) else result.to_json
                raw.append(json.loads(serialized))
            else:
                raw.append(make_json_safe(result))
        texts = list(dict.fromkeys(collect_text(raw)))
        return {"text": "\n".join(texts), "raw": raw}
    finally:
        Path(tmp_path).unlink(missing_ok=True)
