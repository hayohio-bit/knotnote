# KnotNote — 프로젝트 전체 정리

> 작성일: 2026년 4월 23일  
> 목적: 프로젝트 현황 파악 및 향후 기능 기획을 위한 종합 문서

---

## 1. 프로젝트 개요

### 컨셉
KnotNote는 **연결(Knot) 중심의 개인 지식 관리 메모 서비스**다.  
단순히 메모를 저장하는 것을 넘어, 노트 간 관계를 맺고 지식이 서로 연결되는 경험을 제공하는 것이 핵심 아이덴티티다. 이름의 "Knot(매듭)"은 지식을 묶고 연결한다는 의미를 담는다.

### 로고 및 브랜딩
- 로고: 🪢 (rope knot emoji)
- 컬러: 민트 그린 계열 뉴모피즘 (`#1a7a5e` accent, `#f0f4f2` background)
- 디자인 언어: Neumorphism (부드러운 그림자로 입체감 표현)

### 현재 개발 단계
개인 사이드 프로젝트 / 포트폴리오. 핵심 CRUD 및 UI가 구현된 상태이며, 차별화 기능(킥) 개발을 앞두고 있다.

---

## 2. 기술 스택

### 백엔드
| 항목 | 기술 |
|------|------|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 3.2.5 |
| ORM | Spring Data JPA |
| 데이터베이스 | MySQL (운영), H2 (로컬/테스트) |
| 인증 | Spring Security + JWT (jjwt 0.12.3) |
| 문서화 | Springdoc OpenAPI 2.3.0 (Swagger UI) |
| 빌드 | Gradle |
| 기타 | Lombok, Bean Validation |

### 프론트엔드
| 항목 | 기술 |
|------|------|
| 언어 | JavaScript (JSX) |
| 프레임워크 | React 18.3 |
| 번들러 | Vite 5.2 |
| 라우팅 | React Router v6 |
| HTTP | Axios 1.7 |
| UI 라이브러리 | 없음 (순수 CSS, 커스텀 컴포넌트) |
| 외부 패키지 | 최소화 (react-markdown 제거함) |

---

## 3. 아키텍처

### 전체 구조
```
[React Frontend]  ←→  [Spring Boot REST API]  ←→  [MySQL DB]
  :5173 (dev)              :8080                  
```

### 백엔드 레이어 구조
```
Controller → Service (Interface + Impl) → Repository → Entity
                ↕
           DTO (Request / Response)
```

### 프론트엔드 구조
```
src/
├── api/          # axios 인스턴스 + API 모듈 (auth, notes, tags, search)
├── components/   # 재사용 컴포넌트
├── pages/        # 페이지 컴포넌트
├── store/        # 전역 상태 (AuthContext)
└── styles/       # 글로벌 CSS
```

### 인증 흐름
```
로그인 → Access Token (15분) + Refresh Token (7일) 발급
         ↓
    localStorage 저장
         ↓
    Axios 인터셉터 → 모든 요청에 Bearer 토큰 자동 첨부
         ↓
    401 응답 → Refresh Token으로 자동 재발급 → 실패 시 로그아웃
```

---

## 4. 데이터베이스 스키마

### 엔티티 관계도 (ERD 요약)
```
User (1) ──── (N) Note
User (1) ──── (N) Tag
Note (N) ──── (N) Tag        [via NoteTag]
Note (N) ──── (N) Note       [via NoteLink]
Note (1) ──── (1) NoteEmbedding  [AI 준비]
User (1) ──── (1) RefreshToken
```

### 테이블 상세

#### users
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| email | VARCHAR UNIQUE | 로그인 ID |
| password | VARCHAR | BCrypt 해시 |
| nickname | VARCHAR(50) | 표시 이름 |
| role | ENUM(USER) | 현재 USER만 |
| created_at | DATETIME | |
| updated_at | DATETIME | |

#### notes
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT FK | |
| title | VARCHAR NOT NULL | |
| content | LONGTEXT | 마크다운 원문 |
| is_deleted | BOOLEAN | 소프트 삭제 |
| created_at | DATETIME | |
| updated_at | DATETIME | |

#### tags
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT FK | 사용자별 태그 |
| name | VARCHAR | |

#### note_tags
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| note_id | BIGINT FK | |
| tag_id | BIGINT FK | |

#### note_links
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| from_note_id | BIGINT FK | |
| to_note_id | BIGINT FK | |
| UNIQUE(from, to) | | 중복 연결 방지 |

> 링크는 방향이 있지만 조회 시 양방향으로 처리됨.  
> `findAllByNoteId` 쿼리: `fromNote = :id OR toNote = :id`

#### note_embeddings (AI 기반 기능 준비용)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| note_id | BIGINT PK (FK) | |
| embedding | LONGTEXT | 벡터값 JSON 문자열 |
| updated_at | DATETIME | 마지막 임베딩 시각 |

> 엔티티와 테이블이 이미 존재하지만, AI 연동 로직은 미구현 상태.

#### refresh_tokens
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT | |
| token | VARCHAR | |
| expiry_date | DATETIME | |

---

## 5. API 명세

### 인증 (`/api/auth`)
| 메서드 | 경로 | 설명 | 인증 필요 |
|--------|------|------|-----------|
| POST | `/signup` | 회원가입 | ❌ |
| POST | `/login` | 로그인 → Access+Refresh 토큰 | ❌ |
| POST | `/refresh` | 토큰 재발급 | ❌ |

### 사용자 (`/api/users`)
| 메서드 | 경로 | 설명 | 인증 필요 |
|--------|------|------|-----------|
| GET | `/me` | 내 정보 조회 | ✅ |

### 메모 (`/api/notes`)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/` | 메모 목록 (페이징, 최신순) |
| GET | `/{id}` | 메모 상세 (full content + tags + linkedNotes) |
| POST | `/` | 메모 생성 |
| PATCH | `/{id}` | 메모 수정 |
| DELETE | `/{id}` | 메모 소프트 삭제 |
| GET | `/{id}/links` | 연결된 메모 목록 |
| POST | `/{id}/links` | 메모 링크 연결 |
| DELETE | `/{id}/links/{targetId}` | 링크 해제 |

### 태그 (`/api/tags`)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/` | 내 태그 목록 (각 태그의 활성 메모 수 포함) |
| POST | `/` | 태그 생성 |
| DELETE | `/{id}` | 태그 삭제 (NoteTag 매핑도 삭제) |
| POST | `/notes/{noteId}/tags/{tagId}` | 메모에 태그 연결 |
| DELETE | `/notes/{noteId}/tags/{tagId}` | 메모에서 태그 해제 |

### 검색 (`/api/search`)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/?q={keyword}&page=0&size=20` | 키워드 검색 (title + content LIKE) |

### 공통 응답 구조
```json
{
  "status": "success",
  "data": { ... },
  "message": null
}
```

---

## 6. 주요 응답 DTO

### NoteSummaryResponse (목록용)
```json
{
  "id": 1,
  "title": "AWS 배포 체크리스트",
  "preview": "EC2 설정, RDS 연결...",  // content 앞 500자
  "tags": [{"id": 1, "name": "DevOps"}],
  "createdAt": "2026-04-23T10:00:00",
  "updatedAt": "2026-04-23T10:30:00"
}
```

### NoteDetailResponse (단건 조회용)
```json
{
  "id": 1,
  "title": "AWS 배포 체크리스트",
  "content": "# AWS 배포 체크리스트\n\n## EC2 설정...",  // 전체 마크다운
  "tags": [{"id": 1, "name": "DevOps"}],
  "linkedNotes": [{"id": 2, "title": "CI/CD 설정", ...}],
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

## 7. 프론트엔드 — 페이지 및 컴포넌트

### 라우팅
| 경로 | 컴포넌트 | 인증 |
|------|----------|------|
| `/` | LandingPage | ❌ |
| `/login` | LoginPage | ❌ |
| `/signup` | SignupPage | ❌ |
| `/dashboard` | DashboardPage | ✅ |
| `/notes/new` | EditorPage | ✅ |
| `/notes/:id` | EditorPage | ✅ |
| `/search` | → `/dashboard` 리다이렉트 | ✅ |

### 주요 컴포넌트

#### DashboardPage (내 메모)
- **인라인 검색**: 타이핑 300ms 후 자동 API 호출 (debounce)
- **정렬**: 최신순 / 오래된순 / 제목 가나다순 / 제목 역순 (localStorage 저장)
- **보기 전환**: 카드 / 리스트 / 피드 (localStorage 저장)
- **태그 필터**: 태그 클릭으로 필터링 (검색 시 숨김)

#### EditorPage (메모 편집)
- **에디터 모드 2종** (localStorage 저장)
  - `서식 모드`: SimpleEditor — 아이폰 메모처럼 제목/부제목/소제목/본문 블록 기반
  - `MD 모드`: MarkdownEditor — 마크다운 툴바 + 미리보기 토글
- 우측 사이드바: 태그 관리, 연결된 메모 목록 (수정 모드만)
- 저장 / 삭제 / 뒤로가기

#### NoteCard (3가지 뷰)
- **카드**: 3컬럼 그리드, 제목 + 2줄 미리보기 + 태그
- **리스트**: 1행 compact — 제목 + 태그 + 날짜
- **피드**: 전체 너비, 제목 크게 + 최대 800자 본문 + 단락 구분

#### SimpleEditor
- contenteditable 기반 블록 에디터
- 블록 타입: `h1`(제목) / `h2`(부제목) / `h3`(소제목) / `p`(본문)
- Enter → 새 본문 블록 / Backspace(줄 시작) → 이전 블록 병합
- 저장 포맷: 마크다운 (`# 제목`, `## 부제목` 등)
- 한글 IME 조합 처리 (compositionstart/end)

#### MarkdownEditor
- 마크다운 툴바 (제목/굵게/기울임/목록/체크박스/코드/구분선/인용)
- 미리보기: 자체 구현 마크다운→HTML 렌더러 (외부 라이브러리 없음)

### 디자인 시스템

#### CSS 변수
```css
--bg: #f0f4f2         /* 배경 */
--accent: #1a7a5e     /* 민트 그린 */
--text: #1e2d27       /* 기본 텍스트 */
--text-secondary: #5a7068
--border: #d0ddd7
--shadow-out: 6px 6px 14px #c8d4cf, -6px -6px 14px #ffffff  /* 뉴모피즘 볼록 */
--shadow-in: inset ...  /* 뉴모피즘 오목 */
--radius: 16px
--font: Pretendard
```

#### 컨테이너
- max-width: `1400px`, padding: `0 40px`
- 피드 뷰: 전체 너비 (`width: 100%`)

---

## 8. 현재 구현된 기능 목록

### ✅ 완료
| 기능 | 상세 |
|------|------|
| 회원가입 / 로그인 | JWT Access + Refresh Token |
| 자동 토큰 갱신 | Axios 인터셉터 → 401 시 자동 재발급 |
| 메모 CRUD | 생성, 조회, 수정, 소프트 삭제 |
| 태그 시스템 | 생성, 삭제, 메모에 추가/제거, 개수 표시 |
| 메모 연결 | 양방향 링크 (from/to 무관하게 조회) |
| 키워드 검색 | title + content LIKE 쿼리, 대시보드 인라인 |
| 태그 필터 | 대시보드에서 태그 클릭 필터링 |
| 정렬 | 최신순/오래된순/제목 가나다/역순 |
| 보기 전환 | 카드 / 리스트 / 피드 |
| 서식 에디터 | 아이폰 메모 스타일 블록 에디터 |
| 마크다운 에디터 | 툴바 + 미리보기 (자체 렌더러) |
| 페이지네이션 | 백엔드 페이징 (기본 12개/페이지) |
| Swagger UI | `/swagger-ui.html` |

### ⚠️ 부분 구현 / 알려진 이슈
| 항목 | 상태 |
|------|------|
| SearchServiceImpl preview | 100자 (NoteServiceImpl은 500자로 수정됨, 통일 필요) |
| 백링크 표시 | 백엔드는 이미 양방향 조회 지원. 프론트 "연결된 메모" 섹션이 사실상 백링크 역할을 하지만, "이 노트를 참조하는 메모"라는 명시적 구분 없음 |
| 노트 자동저장 | 없음 (수동 저장 버튼) |
| NoteEmbedding | 엔티티/테이블 존재하지만 실제 AI 연동 없음 |
| 이미지 첨부 | 없음 |
| 모바일 반응형 | 기본 대응만 (1컬럼 전환) |

---

## 9. 경쟁 서비스 분석

| 서비스 | 강점 | 약점 | KnotNote와의 차이점 |
|--------|------|------|---------------------|
| **Notion** | 올인원, 팀 협업, 다양한 블록 | 무겁고 복잡, 느림 | 개인 메모에 집중, 빠름 |
| **Obsidian** | 로컬 저장, 강력한 그래프뷰, 플러그인 | 클라우드 동기화 유료, 진입장벽 높음 | 웹 기반, 더 쉬운 UX |
| **Apple Notes** | UX 직관적, 기기 간 동기화 | 검색/연결 기능 빈약, Apple 생태계 한정 | 연결 기능 + 크로스플랫폼 |
| **Roam Research** | 백링크/그래프 선구자 | 가격 비싸고 UI 어려움 | 접근성 높은 대안 |
| **Logseq** | 오픈소스, 아웃라이너 | 복잡한 개념, 서버 동기화 없음 | 더 단순하고 직관적 |

**KnotNote의 포지셔닝**: "Obsidian의 그래프뷰 + Apple Notes의 직관적 UX + 웹 기반 접근성"

---

## 10. 향후 기능 로드맵

### Phase 3 — 킥(Kick) 기능: 연결의 시각화

#### 🔗 백링크 패널 (Backlinks) — 즉시 구현 가능
**개념**: 현재 보고 있는 메모를 참조하는 다른 메모들을 자동으로 표시.  
**현황**: 백엔드는 이미 양방향 조회를 지원함. 프론트 에디터 사이드바의 "연결된 메모"를 "이 메모에서 연결" / "이 메모를 참조하는 메모" 두 섹션으로 분리하면 됨.  
**구현 난이도**: 낮음 — 프론트 UI 수정만 필요  
**임팩트**: 중간 — 지식 관리의 양방향성을 체감하게 해줌

#### 🗺️ 노트 지도 (Knowledge Graph) — 킥 기능의 핵심
**개념**: 전체 메모를 노드, 링크를 엣지로 시각화한 인터랙티브 그래프.  
**UX**: 노드 클릭 → 메모 열기 / 드래그로 위치 조정 / 태그별 색상 클러스터  
**구현**: D3.js (force simulation) 또는 Cytoscape.js  
**필요 API**: `GET /api/graph` → 전체 노트 ID/제목 + 링크 관계 반환  
**임팩트**: 최상 — 차별화 포인트, 시연 시 "wow" 반응 확실  
**구현 난이도**: 중간

```
필요한 백엔드 API 응답 예시:
{
  "nodes": [
    {"id": 1, "title": "AWS 배포", "tags": ["DevOps"]},
    {"id": 2, "title": "CI/CD 설정", "tags": ["DevOps"]}
  ],
  "edges": [
    {"source": 1, "target": 2}
  ]
}
```

#### 💡 스마트 연결 제안 (Smart Suggestions) — 아하! 모멘트
**개념**: 메모 편집 중 "이런 메모와 연결하면 어떨까요?" 자동 제안.  
**구현 방법 A (즉시)**: 공통 태그 수 + 키워드 겹침으로 유사도 계산  
**구현 방법 B (AI)**: NoteEmbedding 벡터로 코사인 유사도 계산 — 엔티티가 이미 준비되어 있음  
**임팩트**: 높음 — 사용자가 몰랐던 연결을 발견하는 경험

### Phase 4 — 사용성 강화

#### ⚡ 빠른 캡처 (Quick Capture)
**개념**: 어느 페이지에서든 `Ctrl+K` (또는 플로팅 버튼)로 미니 메모 모달 열기.  
**저장**: 제목 없이 내용만 → "미분류" 임시 저장 → 나중에 정리  
**임팩트**: 사용 빈도 극적으로 증가. 메모는 마찰이 없어야 한다.

#### 📅 데일리 노트 (Daily Note)
**개념**: 날짜를 제목으로 하는 노트가 자동 생성. 매일 여는 이유를 만들어줌.  
**UX**: 네비바에 "오늘" 버튼 → 오늘 날짜 메모 없으면 자동 생성 후 열기  
**백엔드**: `GET /api/notes/daily?date=2026-04-23` 엔드포인트 추가  
**임팩트**: 일일 활성 사용자(DAU) 증가

#### 🔄 자동 저장 (Autosave)
**개념**: 타이핑 멈춤 1.5초 후 자동 저장.  
**구현**: debounce + `notesApi.update()` 자동 호출, "저장됨" 인디케이터  
**임팩트**: 저장 불안 해소, UX 완성도 향상

#### 📋 메모 템플릿 (Templates)
**개념**: 새 메모 생성 시 템플릿 선택 — 회의록 / 독서 기록 / TIL / 회고 등  
**구현**: 프론트에 하드코딩 (백엔드 불필요)  
**임팩트**: 첫 진입 장벽 낮춤, 아이디어가 없어도 바로 시작 가능

### Phase 5 — AI 기능 (NoteEmbedding 활용)
NoteEmbedding 엔티티가 이미 준비되어 있어 AI 기능 추가를 위한 인프라는 갖춰져 있다.

| 기능 | 설명 | 구현 방법 |
|------|------|-----------|
| AI 태그 추천 | 메모 내용 기반 태그 자동 제안 | Claude / GPT API |
| AI 요약 | 긴 메모 3줄 요약 | Claude API |
| 시맨틱 검색 | 의미 기반 유사 메모 검색 | embedding + cosine similarity |
| 연결 자동 제안 | 벡터 유사도로 관련 메모 추천 | NoteEmbedding 활용 |

---

## 11. 기술적 개선 포인트

### 백엔드
- `SearchServiceImpl`의 preview 길이를 500자로 통일 (현재 100자)
- N+1 문제: `toSummary()` 내 `noteTagRepository.findByNoteId()` 호출이 메모 수만큼 발생 → `@EntityGraph` 또는 fetch join으로 개선
- 삭제된 메모 영구 삭제 배치 (소프트 삭제 데이터 정리)

### 프론트엔드
- 에디터 자동저장 미구현
- 모바일 에디터 UX 개선 필요
- 에러 핸들링 일부 `alert()` 사용 → Toast 알림으로 개선
- `SimpleEditor`의 엔터/백스페이스 엣지 케이스 추가 테스트 필요

---

## 12. 개발 환경 및 실행

### 백엔드 실행
```bash
# H2 인메모리 DB로 로컬 실행
./gradlew bootRun

# Swagger UI
http://localhost:8080/swagger-ui.html
```

### 프론트엔드 실행
```bash
cd frontend
npm install
npm run dev

# http://localhost:5173
```

### 환경 변수 (백엔드 `application.properties` 또는 환경변수)
```properties
# DB (운영)
spring.datasource.url=jdbc:mysql://localhost:3306/knotnote
spring.datasource.username=...
spring.datasource.password=...

# JWT
jwt.secret=...
jwt.access-expiration=900000   # 15분
jwt.refresh-expiration=604800000  # 7일
```

---

## 13. 프로젝트 파일 구조

```
Knotnote/
├── build.gradle
├── src/
│   ├── main/java/com/knotnote/backend/
│   │   ├── BackendApplication.java
│   │   ├── common/         ApiResponse.java
│   │   ├── config/         SecurityConfig, SwaggerConfig
│   │   ├── controller/     Auth, User, Note, Tag, Search
│   │   ├── dto/
│   │   │   ├── request/    Signup, Login, NoteCreate, NoteUpdate, NoteLink, TagCreate, TokenRefresh
│   │   │   └── response/   Auth, Token, User, NoteDetail, NoteSummary, Tag
│   │   ├── entity/         User, Note, Tag, NoteTag, NoteLink, NoteEmbedding, RefreshToken, BaseTimeEntity, Role
│   │   ├── exception/      ErrorCode, CustomException, GlobalExceptionHandler
│   │   ├── repository/     User, Note, NoteTag, NoteLink, Tag, RefreshToken
│   │   ├── security/       JwtTokenProvider, JwtAuthFilter, UserDetailsService, SecurityUtil
│   │   └── service/        Auth, User, Note, Tag, Search (Interface + Impl)
│   └── main/resources/
│       └── application.properties
└── frontend/
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── App.jsx
        ├── main.jsx
        ├── api/            axios.js, auth.js, notes.js, tags.js, search.js
        ├── components/     Navbar, NoteCard, TagBadge, Spinner, SimpleEditor, MarkdownEditor
        ├── pages/          Landing, Login, Signup, Dashboard, Editor
        ├── store/          AuthContext.jsx
        └── styles/         global.css
```

---

## 14. 요약 — KnotNote의 현재와 미래

### 지금 KnotNote는
개인 지식 관리를 위한 클린한 웹 메모 서비스다. 기본 CRUD, 태그, 검색, 메모 간 연결이 동작하고, 서식 모드 에디터와 3가지 보기 방식으로 좋은 UX를 제공한다.

### KnotNote가 되어야 하는 것
"**내 생각이 어떻게 연결되어 있는지 보여주는 서비스**".  
이름(Knot)이 말하듯, 단순 저장을 넘어 지식의 그물망을 시각화하고, AI로 몰랐던 연결을 발견하게 해주는 개인 지식 그래프 도구.

### 다음 한 걸음
**노트 지도(Knowledge Graph)** 구현 → 백링크 패널 정리 → 빠른 캡처 순으로 진행하면, 현재의 "좋은 메모장"이 "쓰고 싶은 지식 도구"로 전환된다.
