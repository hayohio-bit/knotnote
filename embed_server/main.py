"""
KnotNote Embedding Server
FastAPI + sentence-transformers 기반 임베딩 서버

엔드포인트:
  GET  /health                     → 서버 상태 확인
  POST /embed                      → 텍스트 1개 임베딩
  POST /embed/batch                → 텍스트 여러 개 일괄 임베딩
  POST /similarity                 → 두 임베딩 간 코사인 유사도 계산

실행:
  pip install -r requirements.txt
  uvicorn main:app --host 0.0.0.0 --port 8000

모델: paraphrase-multilingual-MiniLM-L12-v2
  - 384차원, 50+ 언어(한국어 포함)
  - 모델 크기 ~420MB, 로드 시간 약 5~10초
"""

from contextlib import asynccontextmanager
from typing import List, Optional

import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

# ── 모델 로드 ──────────────────────────────────────────────────────────────

MODEL_NAME = "paraphrase-multilingual-MiniLM-L12-v2"
model: Optional[SentenceTransformer] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global model
    print(f"[startup] Loading model: {MODEL_NAME}")
    model = SentenceTransformer(MODEL_NAME)
    print("[startup] Model loaded successfully")
    yield
    print("[shutdown] Server shutting down")


app = FastAPI(
    title="KnotNote Embedding Server",
    description="Multilingual text embedding server for KnotNote semantic search",
    version="1.0.0",
    lifespan=lifespan,
)


# ── 요청/응답 스키마 ────────────────────────────────────────────────────────

class EmbedRequest(BaseModel):
    text: str


class EmbedResponse(BaseModel):
    embedding: List[float]
    dim: int


class BatchEmbedRequest(BaseModel):
    texts: List[str]


class BatchEmbedResponse(BaseModel):
    embeddings: List[List[float]]
    dim: int
    count: int


class SimilarityRequest(BaseModel):
    embedding_a: List[float]
    embedding_b: List[float]


class SimilarityResponse(BaseModel):
    similarity: float


class HealthResponse(BaseModel):
    status: str
    model: str
    dim: int


# ── 헬퍼 ───────────────────────────────────────────────────────────────────

def cosine_similarity(a: List[float], b: List[float]) -> float:
    """정규화된 벡터의 코사인 유사도 (= 내적)"""
    va = np.array(a, dtype=np.float32)
    vb = np.array(b, dtype=np.float32)
    norm_a = np.linalg.norm(va)
    norm_b = np.linalg.norm(vb)
    if norm_a == 0.0 or norm_b == 0.0:
        return 0.0
    return float(np.dot(va, vb) / (norm_a * norm_b))


def encode(text: str) -> List[float]:
    """텍스트를 L2 정규화된 임베딩 벡터로 변환"""
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded yet")
    vec = model.encode(text, normalize_embeddings=True, show_progress_bar=False)
    return vec.tolist()


# ── 엔드포인트 ─────────────────────────────────────────────────────────────

@app.get("/health", response_model=HealthResponse)
def health():
    """서버 및 모델 상태 확인"""
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded yet")
    # 모델 출력 차원 확인
    dim = model.get_sentence_embedding_dimension()
    return HealthResponse(status="ok", model=MODEL_NAME, dim=dim)


@app.post("/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest):
    """
    텍스트 1개를 임베딩 벡터로 변환 (L2 정규화 적용)

    - 빈 텍스트는 400 반환
    - 결과 벡터는 L2 정규화되어 있으므로 내적 = 코사인 유사도
    """
    text = req.text.strip()
    if not text:
        raise HTTPException(status_code=400, detail="text must not be empty")
    embedding = encode(text)
    return EmbedResponse(embedding=embedding, dim=len(embedding))


@app.post("/embed/batch", response_model=BatchEmbedResponse)
def embed_batch(req: BatchEmbedRequest):
    """
    텍스트 여러 개를 한 번에 임베딩 (배치 처리로 GPU/CPU 효율 향상)

    - 빈 배열은 400 반환
    - 각 텍스트는 빈 문자열도 허용 (빈 텍스트는 영벡터 반환)
    """
    if not req.texts:
        raise HTTPException(status_code=400, detail="texts must not be empty")
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded yet")

    vecs = model.encode(req.texts, normalize_embeddings=True, show_progress_bar=False)
    embeddings = [v.tolist() for v in vecs]
    dim = len(embeddings[0]) if embeddings else 0
    return BatchEmbedResponse(embeddings=embeddings, dim=dim, count=len(embeddings))


@app.post("/similarity", response_model=SimilarityResponse)
def similarity(req: SimilarityRequest):
    """
    두 임베딩 벡터 간의 코사인 유사도를 계산

    - Spring Boot 측에서 직접 계산할 수도 있지만,
      Python에서 numpy로 계산하는 것이 더 정확함
    - 반환값: [-1.0, 1.0] (실제로는 [0.0, 1.0] 범위)
    """
    if len(req.embedding_a) != len(req.embedding_b):
        raise HTTPException(
            status_code=400,
            detail=f"Dimension mismatch: {len(req.embedding_a)} vs {len(req.embedding_b)}"
        )
    sim = cosine_similarity(req.embedding_a, req.embedding_b)
    return SimilarityResponse(similarity=sim)
