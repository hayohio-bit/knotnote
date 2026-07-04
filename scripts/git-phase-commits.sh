#!/usr/bin/env bash
# ============================================================
# KnotNote - Phase별 Git Commit 스크립트 (Git Bash용)
# 사용법:
#   cd /e/workspace/Knotnote
#   bash git-phase-commits.sh
# ============================================================

set -e
export LANG=en_US.UTF-8
export GIT_TERMINAL_PROMPT=0

cd "$(dirname "$0")/.."

# ── 기존 .git 초기화 ─────────────────────────────────────────
if [ -d ".git" ]; then
    echo "기존 .git 삭제..."
    rm -rf .git
fi

git init -b main
git config user.email "hayohio@gmail.com"
git config user.name  "Serena"
git config i18n.commitEncoding    utf-8
git config i18n.logOutputEncoding utf-8
git config core.quotepath         false
echo "✅ git 초기화 완료"

# ── 헬퍼: UTF-8 임시파일로 커밋 ─────────────────────────────
do_commit() {
    local msg="$1"
    if git diff --cached --name-only | grep -q .; then
        local tmp
        tmp=$(mktemp)
        printf '%s' "$msg" > "$tmp"
        git commit -F "$tmp"
        rm -f "$tmp"
        echo "  📦 커밋: $(echo "$msg" | head -1)"
    else
        echo "  (스킵) 스테이징 없음"
    fi
}

# ── git add 래퍼: 없는 경로 무시 ─────────────────────────────
add_path() {
    for p in "$@"; do
        [ -e "$p" ] && git add "$p"
    done
}

# ════════════════════════════════════════════════════════════
# Phase 1 — 기반, Auth/JWT, 노트 CRUD, 태그, SmartFolder
# ════════════════════════════════════════════════════════════
echo ""
echo "── Phase 1 스테이징..."

add_path \
    build.gradle settings.gradle gradlew gradlew.bat gradle \
    .gitignore docker-compose.yml db \
    src/main/resources/application.yml \
    src/main/java/com/knotnote/backend/BackendApplication.java \
    src/main/java/com/knotnote/backend/common \
    src/main/java/com/knotnote/backend/exception \
    src/main/java/com/knotnote/backend/security \
    src/main/java/com/knotnote/backend/config/SecurityConfig.java \
    src/main/java/com/knotnote/backend/config/SwaggerConfig.java \
    src/main/java/com/knotnote/backend/config/RestTemplateConfig.java \
    src/main/java/com/knotnote/backend/entity/BaseTimeEntity.java \
    src/main/java/com/knotnote/backend/entity/User.java \
    src/main/java/com/knotnote/backend/entity/Role.java \
    src/main/java/com/knotnote/backend/entity/Note.java \
    src/main/java/com/knotnote/backend/entity/NoteLink.java \
    src/main/java/com/knotnote/backend/entity/NoteTag.java \
    src/main/java/com/knotnote/backend/entity/Tag.java \
    src/main/java/com/knotnote/backend/entity/SmartFolder.java \
    src/main/java/com/knotnote/backend/entity/RefreshToken.java \
    src/main/java/com/knotnote/backend/repository/UserRepository.java \
    src/main/java/com/knotnote/backend/repository/NoteRepository.java \
    src/main/java/com/knotnote/backend/repository/NoteLinkRepository.java \
    src/main/java/com/knotnote/backend/repository/NoteTagRepository.java \
    src/main/java/com/knotnote/backend/repository/TagRepository.java \
    src/main/java/com/knotnote/backend/repository/SmartFolderRepository.java \
    src/main/java/com/knotnote/backend/repository/RefreshTokenRepository.java \
    src/main/java/com/knotnote/backend/dto/request/LoginRequest.java \
    src/main/java/com/knotnote/backend/dto/request/SignupRequest.java \
    src/main/java/com/knotnote/backend/dto/request/TokenRefreshRequest.java \
    src/main/java/com/knotnote/backend/dto/request/NoteCreateRequest.java \
    src/main/java/com/knotnote/backend/dto/request/NoteUpdateRequest.java \
    src/main/java/com/knotnote/backend/dto/request/NoteLinkRequest.java \
    src/main/java/com/knotnote/backend/dto/request/CrystallizeRequest.java \
    src/main/java/com/knotnote/backend/dto/request/SmartFolderRequest.java \
    src/main/java/com/knotnote/backend/dto/request/TagCreateRequest.java \
    src/main/java/com/knotnote/backend/dto/response/AuthResponse.java \
    src/main/java/com/knotnote/backend/dto/response/TokenResponse.java \
    src/main/java/com/knotnote/backend/dto/response/UserResponse.java \
    src/main/java/com/knotnote/backend/dto/response/NoteDetailResponse.java \
    src/main/java/com/knotnote/backend/dto/response/NoteSummaryResponse.java \
    src/main/java/com/knotnote/backend/dto/response/PendingLinkResponse.java \
    src/main/java/com/knotnote/backend/dto/response/CrystallizeResponse.java \
    src/main/java/com/knotnote/backend/dto/response/SmartFolderResponse.java \
    src/main/java/com/knotnote/backend/dto/response/TagResponse.java \
    src/main/java/com/knotnote/backend/dto/response/GraphResponse.java \
    src/main/java/com/knotnote/backend/service/AuthService.java \
    src/main/java/com/knotnote/backend/service/AuthServiceImpl.java \
    src/main/java/com/knotnote/backend/service/NoteService.java \
    src/main/java/com/knotnote/backend/service/NoteServiceImpl.java \
    src/main/java/com/knotnote/backend/service/TagService.java \
    src/main/java/com/knotnote/backend/service/TagServiceImpl.java \
    src/main/java/com/knotnote/backend/service/SmartFolderService.java \
    src/main/java/com/knotnote/backend/service/SmartFolderServiceImpl.java \
    src/main/java/com/knotnote/backend/service/UserService.java \
    src/main/java/com/knotnote/backend/service/UserServiceImpl.java \
    src/main/java/com/knotnote/backend/controller/AuthController.java \
    src/main/java/com/knotnote/backend/controller/NoteController.java \
    src/main/java/com/knotnote/backend/controller/TagController.java \
    src/main/java/com/knotnote/backend/controller/SmartFolderController.java \
    src/main/java/com/knotnote/backend/controller/UserController.java \
    src/test/java/com/knotnote/backend/AuthIntegrationTest.java \
    src/test/java/com/knotnote/backend/NoteIntegrationTest.java \
    src/test/java/com/knotnote/backend/NoteLinkIntegrationTest.java \
    src/test/java/com/knotnote/backend/TagIntegrationTest.java \
    src/test/java/com/knotnote/backend/SmartFolderIntegrationTest.java

[ -d src/test/resources ] && git add src/test/resources

do_commit "feat(phase1): 프로젝트 기반 - Auth/JWT, 노트 CRUD, 태그, SmartFolder

- JWT 발급·갱신·블랙리스트(RefreshToken)
- 노트 CRUD, 소프트 삭제, 링크 관리, Crystallize Mode
- 태그 CRUD, 스마트 폴더
- 통합 테스트: Auth / Note / NoteLink / Tag / SmartFolder"

# ════════════════════════════════════════════════════════════
# Phase 2 — KnotVitality, 임베딩, 시맨틱 검색, Knot Decay
# ════════════════════════════════════════════════════════════
echo "── Phase 2 스테이징..."

add_path \
    embed_server \
    src/main/java/com/knotnote/backend/embedding \
    src/main/java/com/knotnote/backend/entity/NoteEmbedding.java \
    src/main/java/com/knotnote/backend/repository/NoteEmbeddingRepository.java \
    src/main/java/com/knotnote/backend/service/EmbeddingService.java \
    src/main/java/com/knotnote/backend/service/EmbeddingServiceImpl.java \
    src/main/java/com/knotnote/backend/service/KnotVitalityService.java \
    src/main/java/com/knotnote/backend/service/SearchService.java \
    src/main/java/com/knotnote/backend/service/SearchServiceImpl.java \
    src/main/java/com/knotnote/backend/dto/response/DecayAlertResponse.java \
    src/main/java/com/knotnote/backend/dto/response/RecommendationResponse.java \
    src/main/java/com/knotnote/backend/controller/SearchController.java \
    src/test/java/com/knotnote/backend/KnotVitalityIntegrationTest.java \
    src/test/java/com/knotnote/backend/SearchIntegrationTest.java

do_commit "feat(phase2): KnotVitality, 임베딩 서버, 시맨틱 검색, Knot Decay 알림

- Python embed_server (FastAPI + sentence-transformers)
- NoteEmbedding 엔티티, EmbeddingClient (fail-safe 폴백)
- KnotVitalityService: Knot Strength Score, Vitality Score 계산
- 시맨틱 검색 (GET /api/search)
- Knot Decay 알림 (GET /api/notes/decay-alerts)
- 스마트 연결 추천 (Jaccard + 그래프 코사인)
- 통합 테스트: KnotVitality / Search"

# ════════════════════════════════════════════════════════════
# Phase 3 — Stats Dashboard, AI 태그 추천, Logout, Profile
# ════════════════════════════════════════════════════════════
echo "── Phase 3 스테이징..."

add_path \
    src/main/java/com/knotnote/backend/dto/request/LogoutRequest.java \
    src/main/java/com/knotnote/backend/dto/request/UpdateProfileRequest.java \
    src/main/java/com/knotnote/backend/dto/response/StatsResponse.java \
    src/main/java/com/knotnote/backend/dto/response/TagSuggestionResponse.java \
    src/main/java/com/knotnote/backend/service/StatsService.java \
    src/main/java/com/knotnote/backend/service/StatsServiceImpl.java \
    src/main/java/com/knotnote/backend/controller/StatsController.java \
    src/test/java/com/knotnote/backend/Phase3IntegrationTest.java

do_commit "feat(phase3): Stats Dashboard, AI 태그 추천, Logout, Profile Update

- Stats Dashboard (GET /api/stats): 노트·링크·태그 집계, Vitality 분포
- Graph Insights (GET /api/stats/graph-insights): orphan/hub/weak-link, BFS 클러스터
- AI 태그 추천 (GET /api/notes/{id}/tag-suggestions): 임베딩 코사인·Jaccard 폴백
- Logout (POST /api/auth/logout): RefreshToken 블랙리스트
- Profile Update (PATCH /api/users/me)
- 통합 테스트 25개"

# ════════════════════════════════════════════════════════════
# Phase 4 — 버전 이력, Graph Insights, ZIP 내보내기, 스케줄러
# ════════════════════════════════════════════════════════════
echo "── Phase 4 스테이징..."

add_path \
    src/main/java/com/knotnote/backend/config/AsyncConfig.java \
    src/main/java/com/knotnote/backend/entity/NoteVersion.java \
    src/main/java/com/knotnote/backend/repository/NoteVersionRepository.java \
    src/main/java/com/knotnote/backend/dto/response/NoteVersionResponse.java \
    src/main/java/com/knotnote/backend/dto/response/GraphInsightsResponse.java \
    src/main/java/com/knotnote/backend/service/GraphInsightsService.java \
    src/main/java/com/knotnote/backend/service/GraphInsightsServiceImpl.java \
    src/main/java/com/knotnote/backend/service/ExportService.java \
    src/main/java/com/knotnote/backend/service/ExportServiceImpl.java \
    src/main/java/com/knotnote/backend/controller/ExportController.java \
    src/main/java/com/knotnote/backend/scheduler/VitalityRefreshScheduler.java \
    src/test/java/com/knotnote/backend/Phase4IntegrationTest.java

do_commit "feat(phase4): 버전 이력, Graph Insights, ZIP 내보내기, Vitality 자동 갱신

- 노트 버전 이력 (GET /api/notes/{id}/versions): 수정 전 스냅샷 자동 저장
- 버전 복원 (POST /api/notes/{id}/versions/{vId}/restore)
- Graph Insights: BFS 클러스터, orphan/hub/weak-link 심층 분석
- ZIP 내보내기 (GET /api/export?format=json|markdown)
- @Scheduled Vitality 자동 갱신 (매일 03:00 KST)
- 통합 테스트 20개"

# ════════════════════════════════════════════════════════════
# Phase 5 — 핀, Activity Feed, 벌크 작업, Low-Vitality 목록
# ════════════════════════════════════════════════════════════
echo "── Phase 5 스테이징..."

add_path \
    src/main/java/com/knotnote/backend/entity/ActivityLog.java \
    src/main/java/com/knotnote/backend/repository/ActivityLogRepository.java \
    src/main/java/com/knotnote/backend/dto/request/BulkDeleteRequest.java \
    src/main/java/com/knotnote/backend/dto/request/BulkTagRequest.java \
    src/main/java/com/knotnote/backend/dto/response/ActivityResponse.java \
    src/main/java/com/knotnote/backend/service/ActivityService.java \
    src/main/java/com/knotnote/backend/service/ActivityServiceImpl.java \
    src/main/java/com/knotnote/backend/controller/ActivityController.java \
    src/test/java/com/knotnote/backend/Phase5IntegrationTest.java

do_commit "feat(phase5): 노트 핀, Activity Feed, 벌크 작업, Low-Vitality 목록

- 노트 상단 고정 (POST/DELETE /api/notes/{id}/pin)
- Activity Feed (GET /api/activity): 11가지 이벤트 타입, 최신순
- 벌크 삭제 (POST /api/notes/bulk/delete)
- 벌크 태그 부착 (POST /api/notes/bulk/tag)
- Low-Vitality 노트 목록 (GET /api/notes/low-vitality?threshold=0.3)
- 통합 테스트 26개"

# ── 나머지 파일 ───────────────────────────────────────────────
if [ -n "$(git status --short)" ]; then
    echo "── 기타 파일 스테이징..."
    git add .
    do_commit "chore: 기타 설정 파일 및 문서 추가"
fi

# ════════════════════════════════════════════════════════════
# Remote 연결 및 Force Push
# ════════════════════════════════════════════════════════════
echo ""
echo "커밋 로그:"
git log --oneline

echo ""
echo "GitHub force push 중..."
git remote add origin "https://github.com/hayohio-bit/knotnote.git"
git push --force -u origin main

echo ""
echo "✅ 완료! https://github.com/hayohio-bit/knotnote"

# ════════════════════════════════════════════════════════════
# Phase 6 — Frontend 연동 (Pin UI, 버전 이력, Activity, Stats, Bulk)
# ════════════════════════════════════════════════════════════
echo "── Phase 6 스테이징..."

add_path \
    frontend/src/api/notes.js \
    frontend/src/pages/EditorPage.jsx \
    frontend/src/pages/EditorPage.css \
    frontend/src/pages/ActivityPage.jsx \
    frontend/src/pages/ActivityPage.css \
    frontend/src/pages/StatsPage.jsx \
    frontend/src/pages/StatsPage.css \
    frontend/src/pages/DashboardPage.jsx \
    frontend/src/pages/DashboardPage.css \
    frontend/src/components/NoteCard.jsx \
    frontend/src/components/NoteCard.css \
    frontend/src/App.jsx \
    frontend/src/components/Navbar.jsx

do_commit "feat(phase6): 프론트엔드 Phase 4-5 기능 연동

- notes.js API 모듈: pin, versions, suggestTags, bulkDelete, bulkAddTag, export, activity, stats 추가
- EditorPage: 핀 토글, 자동저장(1.5s debounce), 버전 이력 사이드바, AI 태그 추천
- ActivityPage: 11가지 이벤트 타입, 상대 시간, 더 보기
- StatsPage: 통계 카드, Vitality 분포, Graph Insights, JSON/Markdown 내보내기
- DashboardPage: 벌크 선택 모드, 핀 우선 정렬, 내보내기 버튼
- NoteCard: 벌크 체크박스 오버레이, 핀 배지
- Navbar: Activity·Stats 메뉴 추가, App.jsx 라우트 추가"

# ════════════════════════════════════════════════════════════
# Phase 7 — UX 개선 (Quick Capture, Templates, 백엔드 preview 통일)
# ════════════════════════════════════════════════════════════
echo "── Phase 7 스테이징..."

add_path \
    frontend/src/components/QuickCapture.jsx \
    frontend/src/components/QuickCapture.css \
    frontend/src/components/TemplateModal.jsx \
    frontend/src/components/TemplateModal.css \
    src/main/java/com/knotnote/backend/service/NoteServiceImpl.java \
    src/main/java/com/knotnote/backend/service/SearchServiceImpl.java \
    src/main/java/com/knotnote/backend/repository/NoteRepository.java

do_commit "feat(phase7): Quick Capture, Templates, 백엔드 preview 개선

- QuickCapture: Ctrl+K 전역 단축키, 제목+내용 입력, '저장 후 열기' (Ctrl+Enter)
- TemplateModal: 데일리 노트·회의록·아이디어·독서 노트·빈 노트 5종 템플릿
- DashboardPage: '📋 템플릿' 버튼 추가
- NoteRepository: findLowVitality (threshold DB 필터링), findByUserIdOrderByPinnedAndUpdated
- NoteServiceImpl: getLowVitalityNotes N+1 제거, Recommendation preview PREVIEW_LENGTH 통일
- SearchServiceImpl: preview '...' 접미 제거, isPinned 필드 추가"

# ════════════════════════════════════════════════════════════
# Phase 8 — CI/CD (GitHub Actions + Docker)
# ════════════════════════════════════════════════════════════
echo "── Phase 8 스테이징..."

add_path \
    .github \
    Dockerfile \
    frontend/Dockerfile \
    frontend/nginx.conf

do_commit "chore(phase8): GitHub Actions CI/CD + Docker

- .github/workflows/ci.yml: backend JUnit 테스트, frontend Vite 빌드, embed_server 구문 검사
- .github/workflows/cd.yml: GHCR에 backend/frontend Docker 이미지 빌드·푸시 (main push 또는 태그)
- Dockerfile (backend): eclipse-temurin 17 멀티스테이지, bootJar
- frontend/Dockerfile: node:20 빌드 → nginx:alpine 서빙
- frontend/nginx.conf: SPA fallback, /api/ 프록시, 정적 파일 캐시"
