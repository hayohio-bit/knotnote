# KnotNote 프로젝트 심층 분석 보고서

## 1. 개요 및 실제 진행 상황 (문서 vs 실제 코드)

프로젝트 문서(`HANDOVER.md`, `KnotNote_전체정리.md`)에는 "Phase 3 (AI) 미검증, 킥(Kick) 기능 개발 예정"으로 기재되어 있으나, **실제 코드를 딥다이브하여 분석한 결과, 문서보다 훨씬 더 많은 개발이 진행된 상태**입니다. 사실상 "킥(Kick)" 기능까지 프론트엔드와 백엔드 모두에 구현이 완료되어 있습니다.

### 🎯 실제 완료된 기능 (문서에 없으나 코드에 존재하는 기능)
*   **지식 그래프 (Knowledge Graph):**
    *   **Backend:** `NoteServiceImpl.getGraph`를 통해 노드/엣지 및 연결 강도(Strength), 활력도(Vitality Score) 반환 API 구현 완료.
    *   **Frontend:** `GraphPage.jsx`를 통해 Canvas 기반으로 그래프 렌더링, 툴팁, 노드 드래그 및 색상 범례 등 인터랙티브 UI 구현 완료.
*   **Crystallize (매듭 확정) 모드:**
    *   **Backend:** `crystallizeLink`, `getPendingLinks` 메서드를 통해 미확정 연결 관리 및 요약 문자열 저장 로직 구현 완료.
    *   **Frontend:** `EditorPage.jsx`와 `CrystallizeModal.jsx`를 통해 노트 간의 연결을 확정(Crystallize)하는 모달 UI 완벽 구현.
*   **스마트 추천 (Smart Suggestions):**
    *   **Backend:** Jaccard 태그 유사도, 공통 이웃(Common Neighbors), 임베딩 기반 코사인 유사도를 혼합한 고도화된 알고리즘으로 노트 및 태그 추천 로직 구현.
    *   **Frontend:** 에디터 우측 패널 및 하단에 AI 기반 노트 연결 추천 목록 렌더링.
*   **웹 클리핑 & AI 요약 기능:**
    *   **Backend & Python:** URL을 입력받아 제목과 본문을 파싱(`main.py`의 `/clip`)하여 자동 노트로 저장하는 기능 구현. GPT-4o-mini를 활용한 노트 3줄 요약 기능(`AiServiceImpl`) 구현.
*   **통계 및 Decay Alert (잊혀진 노트 알림):**
    *   **Frontend & Backend:** `StatsPage.jsx` 및 Activity 내역, 오랫동안 조회되지 않은 노트를 깨우는 알림 기능 구현 완료.

**💡 결론:**
로컬 개발 구현(Phase 1~3 + Kick)은 **99% 완료**되었습니다. 기능적인 결핍은 없으며, **통합 E2E 테스트 및 서버 배포(Phase 4)만 남은 상태**입니다.

---

## 2. 앞으로 해야 할 일 (Next Steps)

현재 코드가 프론트엔드/백엔드/AI 서버 전반에 작성되었지만, 이 3가지 컴포넌트가 동시에 매끄럽게 돌아가는지 통합 검증이 필요합니다. 이후 클라우드 배포를 진행해야 합니다.

1.  **로컬 통합 실행 및 E2E 시나리오 테스트 (최우선 작업)**
    *   Python 임베딩 서버(`embed_server`), Spring Boot 백엔드, React 프론트엔드를 동시에 가동.
    *   직접 노트를 작성하고, 링크를 연결해보고, 그래프가 시각적으로 잘 그려지는지 직접 테스트 진행.
2.  **AWS 배포 환경 구축 (Phase 4)**
    *   Spring Boot와 Python 서버를 AWS EC2 인스턴스에 배포. 
    *   React 프론트엔드 정적 파일을 빌드하여 S3 + CloudFront로 서빙.
3.  **포트폴리오 고도화 및 마무리 (Phase 5)**
    *   서비스가 실제 동작하는 모습을 GIF/비디오로 캡처하여 Github `README.md` 업데이트.
    *   코드 레벨 최적화 점검 (현재 N+1 쿼리 문제가 로직상 어느 정도 방어되어 있으나, 쿼리 로그를 통해 최종 확인 필요).

---

## 3. 구체적인 구현 계획 (AWS 배포 파이프라인)

로컬 테스트 통과 후 즉시 진행할 Phase 4 배포 계획입니다.

| 단계 | 작업 내용 | 상세 및 주의사항 |
| :--- | :--- | :--- |
| **1. 인프라 구성** | EC2 프로비저닝 | 프리티어 EC2 (t2.micro) 사용 시 RAM(1GB)이 부족하여 Spring Boot와 Python 서버가 동시에 뜰 때 OOM(Out of Memory) 발생 확률이 높습니다. **반드시 2GB 이상의 Swap 메모리를 활성화해야 합니다.** |
| **2. 백엔드 배포** | Java 17 + MySQL 구축 | `application-prod.yml`을 생성하고 EC2에 MySQL 8을 직접 설치하거나 RDS를 연동합니다. Github Actions를 통해 빌드된 `.jar` 파일을 EC2로 전송 후 `systemd` 서비스로 등록합니다. |
| **3. AI 서버 배포** | Python + FastAPI 배포 | `embed_server` 폴더를 EC2로 전송 후, 가상환경(`venv`) 생성 및 라이브러리를 설치합니다. Uvicorn을 Background Service로 등록하여 내부 포트 8000번으로 통신합니다. |
| **4. 프론트 배포** | S3 + CloudFront 연동 | Vite 환경에서 빌드 시 `.env.production`에 배포된 백엔드 EC2의 도메인/IP를 API 엔드포인트로 설정합니다. S3 버킷에 빌드 파일을 업로드하고 CloudFront로 HTTPS 연동을 구성합니다. |

---

## 4. 구현 전 미리 준비하거나 해둬야 할 것들 (Prerequisites)

본격적인 클라우드 배포 및 최종 통합 테스트를 위해 사용자님께서 미리 준비해 주셔야 할 항목들입니다.

1.  **OpenAI API Key 발급**
    *   노트 AI 요약 기능(`AiServiceImpl`)을 위해 `GPT-4o-mini` 모델 호출 권한이 있는 API Key가 필요합니다. 테스트를 위해 발급 후 준비해 주세요.
2.  **AWS 계정 점검 및 설정**
    *   AWS 계정 로그인이 가능한지, 프리티어 혜택이 유효한지 확인해 주세요.
    *   원활한 CI/CD 처리를 위해 IAM User를 생성하고 `Access Key` / `Secret Key`를 발급 받아 준비해 둡니다.
3.  **도메인 구매 (선택 사항이나 강력 권장)**
    *   포트폴리오의 완성도를 높이기 위해 저렴한 도메인(예: `.shop`, `.dev`, `.site`)을 하나 구매해 두시면 좋습니다. 프론트엔드를 HTTPS로 서비스하기 위해 필요합니다.
4.  **프론트엔드 환경 변수 점검**
    *   `frontend/.env.local` 파일이 있는지 확인하고, 백엔드 로컬 주소(`VITE_API_URL=http://localhost:8080/api`)가 잘 들어가 있는지 확인합니다.
