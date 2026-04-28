"""
KnotNote Embedding Server
FastAPI + sentence-transformers 기반 임베딩 서버

엔드포인트:
  GET  /health                     → 서버 상태 확인
  POST /embed                      → 텍스트 1개 임베딩
  POST /embed/batch                → 텍스트 여러 개 일괄 임베딩
  POST /similarity                 → 두 임베딩 간 코사인 유사도 계산
  POST /clip                       → URL → 제목·본문 추출 (웹 클리핑)

실행:
  pip install -r requirements.txt
  uvicorn main:app --host 0.0.0.0 --port 8000

모델: paraphrase-multilingual-MiniLM-L12-v2
  - 384차원, 50+ 언어(한국어 포함)
  - 모델 크기 ~420MB, 로드 시간 약 5~10초
"""

from contextlib import asynccontextmanager
from typing import List, Optional

import httpx
import numpy as np
from bs4 import BeautifulSoup
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
    description="Multilingual text embedding & web clipping server for KnotNote",
    version="2.0.0",
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


class ClipRequest(BaseModel):
    url: str


class ClipResponse(BaseModel):
    title: str
    content: str
    source_url: str


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


def extract_main_content(soup: BeautifulSoup) -> str:
    """HTML에서 본문 텍스트 추출 — 불필요한 태그 제거 후 텍스트만"""
    # 스크립트, 스타일, 네비게이션, 푸터 등 제거
    for tag in soup(["script", "style", "nav", "footer", "header",
                     "aside", "form", "button", "noscript", "iframe"]):
        tag.decompose()

    # <article> 또는 <main> 우선 탐색
    main_el = soup.find("article") or soup.find("main") or soup.find("body")
    if main_el is None:
        return ""

    # 텍스트 추출: 줄바꿈 정리
    lines = []
    for element in main_el.find_all(["p", "h1", "h2", "h3", "h4", "h5", "h6", "li", "blockquote"]):
        text = element.get_text(separator=" ", strip=True)
        if len(text) > 20:  # 너무 짧은 조각 제외
            lines.append(text)

    content = "\n\n".join(lines)

    # 최대 5000자 제한 (노트 저장 크기 고려)
    if len(content) > 5000:
        content = content[:5000] + "\n\n[...이후 내용 생략]"

    return content


# ── 엔드포인트 ─────────────────────────────────────────────────────────────

@app.get("/health", response_model=HealthResponse)
def health():
    """서버 및 모델 상태 확인"""
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded yet")
    dim = model.get_sentence_embedding_dimension()
    return HealthResponse(status="ok", model=MODEL_NAME, dim=dim)


@app.post("/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest):
    """
    텍스트 1개를 임베딩 벡터로 변환 (L2 정규화 적용)
    """
    text = req.text.strip()
    if not text:
        raise HTTPException(status_code=400, detail="text must not be empty")
    embedding = encode(text)
    return EmbedResponse(embedding=embedding, dim=len(embedding))


@app.post("/embed/batch", response_model=BatchEmbedResponse)
def embed_batch(req: BatchEmbedRequest):
    """
    텍스트 여러 개를 한 번에 임베딩 (배치 처리)
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
    """
    if len(req.embedding_a) != len(req.embedding_b):
        raise HTTPException(
            status_code=400,
            detail=f"Dimension mismatch: {len(req.embedding_a)} vs {len(req.embedding_b)}"
        )
    sim = cosine_similarity(req.embedding_a, req.embedding_b)
    return SimilarityResponse(similarity=sim)


@app.post("/clip", response_model=ClipResponse)
async def clip(req: ClipRequest):
    """
    URL에서 제목과 본문 텍스트를 추출합니다 (웹 클리핑)

    - User-Agent를 설정하여 일반 브라우저처럼 요청
    - JavaScript 렌더링은 지원하지 않음 (정적 HTML만 파싱)
    - 최대 5000자 본문 반환
    """
    url = req.url.strip()
    if not url.startswith(("http://", "https://")):
        raise HTTPException(status_code=400, detail="URL은 http:// 또는 https://로 시작해야 합니다.")

    headers = {
        "User-Agent": (
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/124.0.0.0 Safari/537.36"
        ),
        "Accept": "text/html,application/xhtml+xml",
        "Accept-Language": "ko-KR,ko;q=0.9,en;q=0.8",
    }

    try:
        async with httpx.AsyncClient(timeout=10.0, follow_redirects=True) as client:
            response = await client.get(url, headers=headers)
            response.raise_for_status()
    except httpx.TimeoutException:
        raise HTTPException(status_code=408, detail="URL 요청 시간이 초과됐습니다.")
    except httpx.HTTPStatusError as e:
        raise HTTPException(status_code=502, detail=f"URL 요청 실패: HTTP {e.response.status_code}")
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"URL 요청 중 오류: {str(e)}")

    # 인코딩 감지
    content_type = response.headers.get("content-type", "")
    if "charset=" in content_type:
        charset = content_type.split("charset=")[-1].split(";")[0].strip()
        html = response.content.decode(charset, errors="replace")
    else:
        html = response.text

    soup = BeautifulSoup(html, "html.parser")

    # 제목 추출
    title = ""
    og_title = soup.find("meta", property="og:title")
    if og_title and og_title.get("content"):
        title = og_title["content"].strip()
    elif soup.title and soup.title.string:
        title = soup.title.string.strip()
    else:
        title = url  # 폴백

    # 본문 추출
    content = extract_main_content(soup)
    if not content:
        # 폴백: 전체 텍스트
        content = soup.get_text(separator="\n", strip=True)[:3000]

    # 출처 URL을 본문 끝에 추가
    content = f"{content}\n\n---\n출처: {url}"

    return ClipResponse(title=title, content=content, source_url=url)
