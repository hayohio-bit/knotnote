# ============================================================
# KnotNote - Phase별 Git Commit 스크립트 (UTF-8 인코딩 보장)
# 사용법:
#   cd E:\workspace\Knotnote
#   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
#   .\git-phase-commits.ps1
# ============================================================

# ── UTF-8 강제 설정 ─────────────────────────────────────────
$OutputEncoding                = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding      = [System.Text.Encoding]::UTF8
[Console]::InputEncoding       = [System.Text.Encoding]::UTF8
$env:LANG                      = "en_US.UTF-8"
$env:GIT_TERMINAL_PROMPT       = "0"
$ErrorActionPreference         = "Continue"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Split-Path -Parent $scriptDir
Set-Location $root

# ── 기존 .git 삭제 후 재초기화 ──────────────────────────────
if (Test-Path ".git") {
    Remove-Item -Recurse -Force ".git"
}
& git init -b main 2>&1 | Out-Null
& git config user.email "hayohio@gmail.com"
& git config user.name  "Serena"
& git config i18n.commitEncoding   utf-8
& git config i18n.logOutputEncoding utf-8
& git config core.quotepath        false
Write-Host "git 초기화 완료" -ForegroundColor Green

# ── 헬퍼: UTF-8 임시파일로 커밋 ────────────────────────────
function Do-Commit {
    param([string]$Message)
    $staged = & git diff --cached --name-only
    if ($staged) {
        $tmp = [System.IO.Path]::GetTempFileName()
        [System.IO.File]::WriteAllText($tmp, $Message, [System.Text.Encoding]::UTF8)
        & git commit -F $tmp | Out-Null
        Remove-Item $tmp -Force
        Write-Host "  커밋됨: $Message" -ForegroundColor Cyan
    } else {
        Write-Host "  (스킵) 스테이징 없음: $Message" -ForegroundColor DarkGray
    }
}

# ── git add 래퍼: 없는 경로 조용히 무시 ─────────────────────
function Add-Path {
    param([string[]]$Paths)
    foreach ($p in $Paths) {
        if (Test-Path $p) {
            & git add $p 2>&1 | Out-Null
        }
    }
}

# ════════════════════════════════════════════════════════════
# Phase 1 — 기반, Auth/JWT, 노트 CRUD, 태그, SmartFolder
# ════════════════════════════════════════════════════════════
Write-Host "`nPhase 1 스테이징..." -ForegroundColor Magenta

Add-Path @(
    "build.gradle","settings.gradle","gradlew","gradlew.bat","gradle",
    ".gitignore","docker-compose.yml","db",
    "src/main/resources/application.yml",
    "src/main/java/com/knotnote/backend/BackendApplication.java",
    "src/main/java/com/knotnote/backend/common",
    "src/main/java/com/knotnote/backend/exception",
    "src/main/java/com/knotnote/backend/security",
    "src/main/java/com/knotnote/backend/config/SecurityConfig.java",
    "src/main/java/com/knotnote/backend/config/SwaggerConfig.java",
    "src/main/java/com/knotnote/backend/config/RestTemplateConfig.java",
    "src/main/java/com/knotnote/backend/entity/BaseTimeEntity.java",
    "src/main/java/com/knotnote/backend/entity/User.java",
    "src/main/java/com/knotnote/backend/entity/Role.java",
    "src/main/java/com/knotnote/backend/entity/Note.java",
    "src/main/java/com/knotnote/backend/entity/NoteLink.java",
    "src/main/java/com/knotnote/backend/entity/NoteTag.java",
    "src/main/java/com/knotnote/backend/entity/Tag.java",
    "src/main/java/com/knotnote/backend/entity/SmartFolder.java",
    "src/main/java/com/knotnote/backend/entity/RefreshToken.java",
    "src/main/java/com/knotnote/backend/repository/UserRepository.java",
    "src/main/java/com/knotnote/backend/repository/NoteRepository.java",
    "src/main/java/com/knotnote/backend/repository/NoteLinkRepository.java",
    "src/main/java/com/knotnote/backend/repository/NoteTagRepository.java",
    "src/main/java/com/knotnote/backend/repository/TagRepository.java",
    "src/main/java/com/knotnote/backend/repository/SmartFolderRepository.java",
    "src/main/java/com/knotnote/backend/repository/RefreshTokenRepository.java",
    "src/main/java/com/knotnote/backend/dto/request/LoginRequest.java",
    "src/main/java/com/knotnote/backend/dto/request/SignupRequest.java",
    "src/main/java/com/knotnote/backend/dto/request/TokenRefreshRequest.java",
    "src/main/java/com/knotnote/backend/dto/request/NoteCreateRequest.java",
    "src/main/java/com/knotnote/backend/dto/request/NoteUpdateRequest.java",
    "src/main/java/com/knotnote/backend/dto/request/NoteLinkRequest.java",
    "src/main/java/com/knotnote/backend/dto/request/CrystallizeRequest.java",
    "src/main/java/com/knotnote/backend/dto/request/SmartFolderRequest.java",
    "src/main/java/com/knotnote/backend/dto/request/TagCreateRequest.java",
    "src/main/java/com/knotnote/backend/dto/response/AuthResponse.java",
    "src/main/java/com/knotnote/backend/dto/response/TokenResponse.java",
    "src/main/java/com/knotnote/backend/dto/response/UserResponse.java",
    "src/main/java/com/knotnote/backend/dto/response/NoteDetailResponse.java",
    "src/main/java/com/knotnote/backend/dto/response/NoteSummaryResponse.java",
    "src/main/java/com/knotnote/backend/dto/response/PendingLinkResponse.java",
    "src/main/java/com/knotnote/backend/dto/response/CrystallizeResponse.java",
    "src/main/java/com/knotnote/backend/dto/response/SmartFolderResponse.java",
    "src/main/java/com/knotnote/backend/dto/response/TagResponse.java",
    "src/main/java/com/knotnote/backend/dto/response/GraphResponse.java",
    "src/main/java/com/knotnote/backend/service/AuthService.java",
    "src/main/java/com/knotnote/backend/service/AuthServiceImpl.java",
    "src/main/java/com/knotnote/backend/service/NoteService.java",
    "src/main/java/com/knotnote/backend/service/NoteServiceImpl.java",
    "src/main/java/com/knotnote/backend/service/TagService.java",
    "src/main/java/com/knotnote/backend/service/TagServiceImpl.java",
    "src/main/java/com/knotnote/backend/service/SmartFolderService.java",
    "src/main/java/com/knotnote/backend/service/SmartFolderServiceImpl.java",
    "src/main/java/com/knotnote/backend/service/UserService.java",
    "src/main/java/com/knotnote/backend/service/UserServiceImpl.java",
    "src/main/java/com/knotnote/backend/controller/AuthController.java",
    "src/main/java/com/knotnote/backend/controller/NoteController.java",
    "src/main/java/com/knotnote/backend/controller/TagController.java",
    "src/main/java/com/knotnote/backend/controller/SmartFolderController.java",
    "src/main/java/com/knotnote/backend/controller/UserController.java",
    "src/test/java/com/knotnote/backend/AuthIntegrationTest.java",
    "src/test/java/com/knotnote/backend/NoteIntegrationTest.java",
    "src/test/java/com/knotnote/backend/NoteLinkIntegrationTest.java",
    "src/test/java/com/knotnote/backend/TagIntegrationTest.java",
    "src/test/java/com/knotnote/backend/SmartFolderIntegrationTest.java"
)
if (Test-Path "src/test/resources") { & git add "src/test/resources" 2>&1 | Out-Null }

Do-Commit "feat(phase1): 프로젝트 기반 - Auth/JWT, 노트 CRUD, 태그, SmartFolder

- JWT 발급·갱신·블랙리스트(RefreshToken)
- 노트 CRUD, 소프트 삭제, 링크 관리, Crystallize Mode
- 태그 CRUD, 스마트 폴더
- 통합 테스트: Auth / Note / NoteLink / Tag / SmartFolder"

# ════════════════════════════════════════════════════════════
# Phase 2 — KnotVitality, 임베딩, 시맨틱 검색, Knot Decay
# ════════════════════════════════════════════════════════════
Write-Host "Phase 2 스테이징..." -ForegroundColor Magenta

Add-Path @(
    "embed_server",
    "src/main/java/com/knotnote/backend/embedding",
    "src/main/java/com/knotnote/backend/entity/NoteEmbedding.java",
    "src/main/java/com/knotnote/backend/repository/NoteEmbeddingRepository.java",
    "src/main/java/com/knotnote/backend/service/EmbeddingService.java",
    "src/main/java/com/knotnote/backend/service/EmbeddingServiceImpl.java",
    "src/main/java/com/knotnote/backend/service/KnotVitalityService.java",
    "src/main/java/com/knotnote/backend/service/SearchService.java",
    "src/main/java/com/knotnote/backend/service/SearchServiceImpl.java",
    "src/main/java/com/knotnote/backend/dto/response/DecayAlertResponse.java",
    "src/main/java/com/knotnote/backend/dto/response/RecommendationResponse.java",
    "src/main/java/com/knotnote/backend/controller/SearchController.java",
    "src/test/java/com/knotnote/backend/KnotVitalityIntegrationTest.java",
    "src/test/java/com/knotnote/backend/SearchIntegrationTest.java"
)

Do-Commit "feat(phase2): KnotVitality, 임베딩 서버, 시맨틱 검색, Knot Decay 알림

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
Write-Host "Phase 3 스테이징..." -ForegroundColor Magenta

Add-Path @(
    "src/main/java/com/knotnote/backend/dto/request/LogoutRequest.java",
    "src/main/java/com/knotnote/backend/dto/request/UpdateProfileRequest.java",
    "src/main/java/com/knotnote/backend/dto/response/StatsResponse.java",
    "src/main/java/com/knotnote/backend/dto/response/TagSuggestionResponse.java",
    "src/main/java/com/knotnote/backend/service/StatsService.java",
    "src/main/java/com/knotnote/backend/service/StatsServiceImpl.java",
    "src/main/java/com/knotnote/backend/controller/StatsController.java",
    "src/test/java/com/knotnote/backend/Phase3IntegrationTest.java"
)

Do-Commit "feat(phase3): Stats Dashboard, AI 태그 추천, Logout, Profile Update

- Stats Dashboard (GET /api/stats): 노트·링크·태그 집계, Vitality 분포
- Graph Insights (GET /api/stats/graph-insights): orphan/hub/weak-link, BFS 클러스터
- AI 태그 추천 (GET /api/notes/{id}/tag-suggestions): 임베딩 코사인·Jaccard 폴백
- Logout (POST /api/auth/logout): RefreshToken 블랙리스트
- Profile Update (PATCH /api/users/me)
- 통합 테스트 25개"

# ════════════════════════════════════════════════════════════
# Phase 4 — 버전 이력, Graph Insights, ZIP 내보내기, 스케줄러
# ════════════════════════════════════════════════════════════
Write-Host "Phase 4 스테이징..." -ForegroundColor Magenta

Add-Path @(
    "src/main/java/com/knotnote/backend/config/AsyncConfig.java",
    "src/main/java/com/knotnote/backend/entity/NoteVersion.java",
    "src/main/java/com/knotnote/backend/repository/NoteVersionRepository.java",
    "src/main/java/com/knotnote/backend/dto/response/NoteVersionResponse.java",
    "src/main/java/com/knotnote/backend/dto/response/GraphInsightsResponse.java",
    "src/main/java/com/knotnote/backend/service/GraphInsightsService.java",
    "src/main/java/com/knotnote/backend/service/GraphInsightsServiceImpl.java",
    "src/main/java/com/knotnote/backend/service/ExportService.java",
    "src/main/java/com/knotnote/backend/service/ExportServiceImpl.java",
    "src/main/java/com/knotnote/backend/controller/ExportController.java",
    "src/main/java/com/knotnote/backend/scheduler/VitalityRefreshScheduler.java",
    "src/test/java/com/knotnote/backend/Phase4IntegrationTest.java"
)

Do-Commit "feat(phase4): 버전 이력, Graph Insights, ZIP 내보내기, Vitality 자동 갱신

- 노트 버전 이력 (GET /api/notes/{id}/versions): 수정 전 스냅샷 자동 저장
- 버전 복원 (POST /api/notes/{id}/versions/{vId}/restore)
- Graph Insights: BFS 클러스터, orphan/hub/weak-link 심층 분석
- ZIP 내보내기 (GET /api/export?format=json|markdown)
- @Scheduled Vitality 자동 갱신 (매일 03:00 KST)
- 통합 테스트 20개"

# ════════════════════════════════════════════════════════════
# Phase 5 — 핀, Activity Feed, 벌크 작업, Low-Vitality 목록
# ════════════════════════════════════════════════════════════
Write-Host "Phase 5 스테이징..." -ForegroundColor Magenta

Add-Path @(
    "src/main/java/com/knotnote/backend/entity/ActivityLog.java",
    "src/main/java/com/knotnote/backend/repository/ActivityLogRepository.java",
    "src/main/java/com/knotnote/backend/dto/request/BulkDeleteRequest.java",
    "src/main/java/com/knotnote/backend/dto/request/BulkTagRequest.java",
    "src/main/java/com/knotnote/backend/dto/response/ActivityResponse.java",
    "src/main/java/com/knotnote/backend/service/ActivityService.java",
    "src/main/java/com/knotnote/backend/service/ActivityServiceImpl.java",
    "src/main/java/com/knotnote/backend/controller/ActivityController.java",
    "src/test/java/com/knotnote/backend/Phase5IntegrationTest.java"
)

Do-Commit "feat(phase5): 노트 핀, Activity Feed, 벌크 작업, Low-Vitality 목록

- 노트 상단 고정 (POST/DELETE /api/notes/{id}/pin)
- Activity Feed (GET /api/activity): 11가지 이벤트 타입, 최신순
- 벌크 삭제 (POST /api/notes/bulk/delete)
- 벌크 태그 부착 (POST /api/notes/bulk/tag)
- Low-Vitality 노트 목록 (GET /api/notes/low-vitality?threshold=0.3)
- 통합 테스트 26개"

# ── 나머지 파일 (문서, 스크립트 등) ─────────────────────────
$untracked = & git status --short
if ($untracked) {
    Write-Host "기타 파일 스테이징..." -ForegroundColor Magenta
    & git add . 2>&1 | Out-Null
    Do-Commit "chore: 기타 설정 파일 및 문서 추가"
}

# ════════════════════════════════════════════════════════════
# Remote 연결 및 Force Push (기존 커밋 덮어쓰기)
# ════════════════════════════════════════════════════════════
Write-Host "`n커밋 로그:" -ForegroundColor Green
& git log --oneline

Write-Host "`nGitHub에 force push 중..." -ForegroundColor Yellow
& git remote add origin "https://github.com/hayohio-bit/knotnote.git"
& git push --force -u origin main

Write-Host "`n완료! https://github.com/hayohio-bit/knotnote" -ForegroundColor Green
