import logging
import os
from typing import Literal

from fastapi import FastAPI, HTTPException, Response
from pydantic import BaseModel, Field
from sentence_transformers import CrossEncoder, SentenceTransformer

app = FastAPI(title="Local Embedding and Reranking Adapter", version="0.1.0")
embedding_model: SentenceTransformer | None = None
rerank_model: CrossEncoder | None = None


class EmbeddingRequest(BaseModel):
    input: list[str] = Field(min_length=1, max_length=128)
    normalize: bool = True


class RerankDocument(BaseModel):
    id: str
    text: str


class RerankRequest(BaseModel):
    query: str = Field(min_length=1)
    documents: list[RerankDocument] = Field(min_length=1, max_length=100)


@app.on_event("startup")
def load_models() -> None:
    global embedding_model, rerank_model
    device = (os.getenv("EMBEDDING_DEVICE") or "").strip() or "cpu"
    try:
        embedding_model = SentenceTransformer(os.environ["EMBEDDING_MODEL"], device=device)
        rerank_name = os.getenv("RERANK_MODEL", "").strip()
        if rerank_name:
            rerank_model = CrossEncoder(rerank_name, device=device)
    except Exception as exc:  # noqa: BLE001 - surface the real cause instead of crash-looping
        logging.exception("Embedding/rerank model failed to load: %s", exc)
        embedding_model, rerank_model = None, None


@app.get("/health")
def health(response: Response) -> dict[str, object]:
    ready = embedding_model is not None
    if not ready:
        response.status_code = 503
    return {
        "status": "ok" if ready else "starting",
        "embedding_model": os.getenv("EMBEDDING_MODEL"),
        "reranker_enabled": rerank_model is not None,
    }


@app.post("/v1/embeddings")
def embeddings(request: EmbeddingRequest) -> dict[str, object]:
    if embedding_model is None:
        raise HTTPException(status_code=503, detail="Embedding model is not ready")
    vectors = embedding_model.encode(
        request.input,
        normalize_embeddings=request.normalize,
        convert_to_numpy=True,
    )
    return {
        "object": "list",
        "data": [{"index": index, "embedding": vector.tolist()} for index, vector in enumerate(vectors)],
        "model": os.getenv("EMBEDDING_MODEL"),
    }


@app.post("/v1/rerank")
def rerank(request: RerankRequest) -> dict[str, object]:
    if rerank_model is None:
        raise HTTPException(status_code=501, detail="Reranker is disabled")
    scores = rerank_model.predict([(request.query, document.text) for document in request.documents])
    ranked = sorted(
        ({"id": document.id, "score": float(score)} for document, score in zip(request.documents, scores)),
        key=lambda item: item["score"],
        reverse=True,
    )
    return {"data": ranked, "model": os.getenv("RERANK_MODEL")}
