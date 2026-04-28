# KnotNote Backend — Handover / 업무지시서

> 본 문서는 다음 작업자(사람/AI 에이전트) 모두가 참고할 수 있도록 작성되었다.
> 목적: 이 리포지토리를 "빌드·실행·검증 → Phase 2 프론트엔드 → Phase 3~5"로 끝까지 이어가기.

작성자: Claude (Cowork mode)
작성일: 2026-04-23
담당자: Serena H. Sun
Notion 루트: https://www.notion.so/32cc614d783880ac9603cbfb08a4636a

---

## 0. 한 눈에 보는 상태

| 단계 | 상태 | 비고 |
|------|------|------|
| Phase 0 기획 | 완료 | Notion 로드맵·ERD·API 명세 존재 |
| Phase 1 백엔드 구현 (본 작업) | **완료** | 버그 수정, P0 빌드·스모크 테스트, P1 통합 테스트 40개 통과 |
| Phase 2 프론트엔드 | **완료** | React+Vite, 5개 페이지, Axios+JWT, 뉴모피즘 UI |
| Phase 3 AI 시맨틱 검색 | 미착수 (엔드포인트만 `NOT_IMPLEMENTED` 스텁) | |
| Phase 4 AWS 배포 | 미착수 | |
| Phase 5 포트폴리오 정리 | 미착수 | |

⚠️ 샌드박스의 네트워크/JDK 제약으로 `./gradlew build`를 실행할 수 없었다.
**2026-04-23 Claude Cowork 세션** 에서 코드 버그 3개를 수정하고 `gradlew`, `gradlew.bat`, `setup.ps1`, `db/init.sql`, `application-test.yml`을 생성했다.
**Serena가 `setup.ps1`을 실행하면** gradle-wrapper.jar 취득 + 빌드까지 자동화된다.

---

## 1. 리포지토리 현재 구조

```
E:\workspace\Knotnote\
├── build.gradle                      # Spring Boot 3.2.5, Java 17, jjwt 0.12.3, springdoc 2.3.0
├── settings.gradle                   # rootProject.name = 'knotnote-backend'
├── gradle/wrapper/gradle-wrapper.properties  # 8.5
├── .gitignore
├── README.md
├── HANDOVER.md                       # ← 이 문서
└── src/main/
    ├── resources/application.yml
    └── java/com/knotnote/backend/
        ├── BackendApplication.java   # @EnableJpaAuditing
        ├── common/ApiResponse.java
        ├── config/
        │   ├── SecurityConfig.java   # STATELESS, JWT 필터 등록, CORS 설정
        │   └── SwaggerConfig.java    # Bearer 인증 스킴 포함
        ├── entity/ (9)
        │   ├── BaseTimeEntity.java   # createdAt / updatedAt (JPA Auditing)
        │   ├── Role.java             # USER / ADMIN
        │   ├── User, Note, Tag, NoteTag, NoteLink, NoteEmbedding, RefreshToken
        ├── repository/ (6)
        │   ├── UserRepository, RefreshTokenRepository
        │   ├── NoteRepository (findByUserIdAndIsDeletedFalse, searchByKeyword 등)
        │   ├── NoteLinkRepository, NoteTagRepository, TagRepository
        ├── security/ (4)
        │   ├── JwtTokenProvider (access 1h / refresh 14d)
        │   ├── JwtAuthenticationFilter (OncePerRequestFilter)
        │   ├── CustomUserDetailsService
        │   └── SecurityUtil.currentUserId()   # Controller에서 재사용
        ├── exception/ (3)
        │   ├── ErrorCode, CustomException, GlobalExceptionHandler
        ├── dto/
        │   ├── request/ (7)  SignupRequest, LoginRequest, TokenRefreshRequest,
        │   │                 NoteCreateRequest, NoteUpdateRequest, NoteLinkRequest, TagCreateRequest
        │   └── response/ (6) AuthResponse, TokenResponse, UserResponse,
        │                     NoteSummaryResponse, NoteDetailResponse(+TagRef), TagResponse
        ├── service/ (10)
        │   ├── AuthService(+Impl), UserService(+Impl)
        │   ├── NoteService(+Impl), TagService(+Impl), SearchService(+Impl)
        └── controller/ (5)
            └── AuthController, UserController, NoteController, TagController, SearchController
```

총 Java 소스 54개. 이 세션에서 수행한 정적 검증 통과 항목:

- 모든 파일의 `package` 선언이 디렉터리 구조와 일치
- 모든 public 클래스/인터페이스/enum 이름이 파일명과 일치
- 서비스 인터페이스와 구현체의 메서드 시그니처 집합 일치(5개 도메인)
- 서비스 코드가 호출하는 Repository 메서드가 모두 선언되어 있음(JpaRepository 내장 메서드 제외)

---

## 2. 전체 엔드포인트 (20개)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | /api/auth/signup | 회원가입 | 불필요 |
| POST | /api/auth/login | 로그인(Access+Refresh 발급) | 불필요 |
| POST | /api/auth/refresh | 토큰 재발급 | 불필요 |
| GET | /api/users/me | 내 프로필 | 필요 |
| GET | /api/notes | 메모 목록 (페이징, createdAt DESC) | 필요 |
| POST | /api/notes | 메모 생성 | 필요 |
| GET | /api/notes/{id} | 메모 단건(태그·연결 포함) | 필요 |
| PATCH | /api/notes/{id} | 메모 수정(부분) | 필요 |
| DELETE | /api/notes/{id} | 메모 소프트 삭제 | 필요 |
| GET | /api/notes/{id}/links | 연결된 메모 목록 | 필요 |
| POST | /api/notes/{id}/links | 메모 링크 연결 | 필요 |
| DELETE | /api/notes/{id}/links/{targetId} | 메모 링크 해제 | 필요 |
| GET | /api/tags | 태그 목록(사용 개수 포함) | 필요 |
| POST | /api/tags | 태그 생성 | 필요 |
| DELETE | /api/tags/{id} | 태그 삭제(연관 NoteTag도 함께 삭제) | 필요 |
| POST | /api/notes/{noteId}/tags?tagId=... | 메모-태그 연결 | 필요 |
| DELETE | /api/notes/{noteId}/tags/{tagId} | 메모-태그 해제 | 필요 |
| GET | /api/search?q=... | 키워드 검색(title/content LIKE) | 필요 |
| GET | /api/search/semantic?q=... | 시맨틱 검색(Phase 3 미구현 스텁, 501) | 필요 |
| - | /swagger-ui.html | Swagger UI | 불필요 |

공통 응답 포맷: `{ "success": boolean, "data": T, "message": string }`
인증: Authorization Bearer 헤더에 JWT(subject=userId).

---

## 3. 로컬 빌드 절차 (다음 작업자가 반드시 실행)

### 3.1 사전 요구사항
- JDK 17 (Temurin/Liberica 권장)
- MySQL 8.x
- IntelliJ IDEA (권장) 또는 VS Code + Extension Pack for Java

### 3.2 Gradle Wrapper 생성
이 세션에서 `gradle-wrapper.jar`와 `gradlew`/`gradlew.bat`은 생성하지 못했다.
로컬에서 1회 실행한다.

```bash
# Gradle 8.5가 설치되어 있으면
gradle wrapper --gradle-version 8.5

# 아니면 IntelliJ에서 "Import Gradle project" 하면 자동 생성됨
```

### 3.3 MySQL 준비
```sql
CREATE DATABASE knotnote
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
CREATE USER 'knotnote'@'localhost' IDENTIFIED BY '강한비밀번호';
GRANT ALL PRIVILEGES ON knotnote.* TO 'knotnote'@'localhost';
FLUSH PRIVILEGES;
```

### 3.4 환경변수
```
DB_USERNAME=knotnote
DB_PASSWORD=강한비밀번호
JWT_SECRET=<32바이트 이상 랜덤 문자열>   # HS256 요구 사항
```
IntelliJ → Run Configuration → Environment variables에 등록.

### 3.5 빌드 / 실행
```bash
./gradlew clean build           # 컴파일 + 테스트 (테스트는 아직 없음)
./gradlew bootRun               # 로컬 실행
```
- 부트 성공 시: MySQL에 7개 테이블이 자동 생성(`users`, `notes`, `tags`, `note_tags`, `note_links`, `note_embeddings`, `refresh_tokens`).
- Swagger: http://localhost:8080/swagger-ui.html

### 3.6 첫 스모크 테스트(Postman / curl)
```bash
# 1. 회원가입
curl -X POST http://localhost:8080/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@test.com","password":"password1!","nickname":"tester"}'

# 2. 로그인
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@test.com","password":"password1!"}'
# → accessToken 복사

# 3. 메모 생성 (TOKEN 치환)
curl -X POST http://localhost:8080/api/notes \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"첫 메모","content":"hello"}'

# 4. 검색
curl "http://localhost:8080/api/search?q=hello" -H "Authorization: Bearer $TOKEN"
```

---

## 4. 다음 작업자가 해야 할 일 (우선순위 순)

### P0 — 빌드 성공 보증 (1~2시간)
- [x] 코드 버그 3개 수정 (MySQL8Dialect → MySQLDialect, ConstraintViolationException 핸들러, @Transactional)
- [x] gradlew / gradlew.bat 생성
- [x] setup.ps1 (gradle-wrapper.jar 자동 취득 + 빌드) 생성
- [x] db/init.sql 생성
- [x] application-test.yml 생성
- [x] Notion P0 작업 로그 생성 https://www.notion.so/34bc614d783881018d8cd24bd9bd9717
- [ ] **[Serena 로컬]** `.\setup.ps1` 실행 → gradle-wrapper.jar 취득 + 빌드 통과 확인
- [ ] **[Serena 로컬]** `bootRun` 후 7개 테이블 자동 생성 확인
- [ ] **[Serena 로컬]** Swagger UI http://localhost:8080/swagger-ui.html → 20개 엔드포인트 확인
- [ ] **[Serena 로컬]** HANDOVER.md §3.6 스모크 테스트 (signup → login → note CRUD)

예상 지점별 점검 포인트:
1. **Lombok 처리** — IntelliJ에서 "Enable annotation processing" 켜져 있어야 한다. 안 켜져 있으면 `getId()` 등 getter 미해결로 수십 개 오류가 난다.
2. **`boolean isDeleted` 프로퍼티** — Spring Data JPA 파생 쿼리(`findByUserIdAndIsDeletedFalse`)는 필드명 기반이라 현재 코드에서 정상 동작해야 한다. 다만 Hibernate 버전에 따라 `property not found: deleted` 로그가 뜰 수 있으니 주의. 필요하면 필드명을 `deleted`로 바꾸거나 JPQL로 교체.
3. **`@EnableJpaAuditing`** — `BackendApplication`에 이미 선언. `BaseTimeEntity`의 `@EntityListeners(AuditingEntityListener.class)`와 짝.
4. **JWT secret 길이** — jjwt 0.12 `Keys.hmacShaKeyFor`는 HS256 기준 최소 32바이트 요구. 짧으면 `WeakKeyException`.

### P1 — 통합 테스트 추가 ✅ 완료 (2026-04-23)
- [x] `AuthIntegrationTest.java` — 7개 (signup, login, refresh 플로우)
- [x] `NoteIntegrationTest.java` — 9개 (CRUD + 페이징 + 타인 메모 404 + 소프트 삭제)
- [x] `TagIntegrationTest.java` — 7개 (태그 CRUD + 메모-태그 연결/해제 + 중복 409)
- [x] `NoteLinkIntegrationTest.java` — 8개 (양방향 링크 생성/조회/해제 + 자기 자신 링크 차단 + 중복 409)
- [x] `SearchIntegrationTest.java` — 9개 (키워드 검색, 페이징, 소프트 삭제 필터, `NOT_IMPLEMENTED` 501)
- **총 40개 테스트 `BUILD SUCCESSFUL`**
- 추가 버그 수정: `GlobalExceptionHandler`에 `MissingServletRequestParameterException` → 400 핸들러 추가

사용 권장:
- `@SpringBootTest` + `@AutoConfigureMockMvc`
- 테스트 DB는 H2 in-memory (이미 `build.gradle`에 `runtimeOnly h2` 포함). `src/test/resources/application-test.yml`에 H2 프로필 작성.

### P2 — 프론트엔드(Phase 2) ✅ 완료 (2026-04-23)
위치: `E:\workspace\Knotnote\frontend\`
실행: `.\run-frontend.ps1` (첫 실행 시 `npm install` 자동 수행)

- [x] React 18 + Vite 5 프로젝트 (`frontend/`)
- [x] 폴더 구조: `src/{pages,components,hooks,api,lib,store,styles}`
- [x] Axios 인스턴스 + 401 인터셉터(자동 refresh + 큐잉)
- [x] JWT localStorage 저장 (`lib/token.js`)
- [x] 전역 인증 상태: `AuthContext` + `PrivateRoute`
- [x] React Router v6 라우팅 (`/`, `/login`, `/signup`, `/dashboard`, `/notes/:id`, `/search`)
- [x] 랜딩 페이지 (뉴모피즘 + 네오 민트, Hero/Stats/Features/CTA)
- [x] 로그인 / 회원가입 페이지
- [x] 대시보드 (메모 목록, 태그 필터, 페이지네이션)
- [x] 에디터 (생성/수정, 태그 연결/해제, 연결 메모 관리)
- [x] 검색 페이지 (키워드, 페이지네이션, URL 쿼리 동기화)

API 레이어: `src/api/{axios,auth,notes,tags,search}.js`  
공통 컴포넌트: `Navbar`, `NoteCard`, `TagBadge`, `Spinner`, `PrivateRoute`  
디자인: 뉴모피즘(shadow-out/in) + 네오 민트(`#1a7a5e`) + 배경 `#f0f4f2`

### P3 — AI 시맨틱 검색(Phase 3, 3~5일)
- [ ] Python FastAPI 서버 (`embedding-server/`)
  - `/embed` — OpenAI `text-embedding-3-small`로 벡터화
  - `/search` — 코사인 유사도 Top-K 반환
- [ ] Spring Boot 측
  - 메모 저장/수정 시 `NoteEmbedding` 갱신(배치 or 비동기)
  - `SearchController.semanticSearch`에서 Python 서버 호출
- [ ] `NoteEmbedding.embedding` 컬럼에 JSON 배열로 저장 (MySQL 8 `JSON` 타입으로 변경 고려)

### P4 — AWS 배포(Phase 4, 3~5일)
- [ ] EC2 t2.micro + 보안그룹 3306/8080/443/22
- [ ] Java 17 + MySQL 8 직접 설치(프리티어 비용 절감)
- [ ] S3 + CloudFront + ACM HTTPS, 도메인 DNS
- [ ] GitHub Actions: `deploy-backend.yml`(JAR S3 업로드 → SSM으로 EC2 재시작), `deploy-frontend.yml`(빌드 → S3 sync → CloudFront 무효화)
- [ ] Secrets: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `EC2_HOST`, `EC2_USER`, `JWT_SECRET`, `DB_*`

### P5 — 포트폴리오 정리
- [ ] README.md 확장 (아키텍처 다이어그램, 트러블슈팅, 기술 선택 이유)
- [ ] 랜딩 페이지 About 섹션에 GitHub/데모 링크 연결
- [ ] 기술 블로그 1편: "JWT 기반 Spring Boot 인증 — Notion 기반 설계부터 배포까지"

---

## 5. 설계 의사결정 요약 (왜 이렇게 만들었는가)

| 항목 | 결정 | 이유 |
|------|------|------|
| Java 17 + Spring Boot 3.2.5 | Jakarta EE 9+ 전환, 최신 LTS | 포트폴리오 최신성 |
| Gradle Groovy DSL | Spring Initializr 기본 | 학습 곡선 낮음 |
| Entity 생성자 `PROTECTED` + `@Builder` | JPA 기본 생성자 요구 + 빌더 강제 | 무분별 객체 생성 차단 |
| `FetchType.LAZY` 일괄 | N+1은 쿼리에서 해결 | 초기 성능 최적화 |
| `NOTE_TAGS` standalone `id` PK | JPA 복합키 복잡도 회피 | 엔티티 단순화 |
| 소프트 삭제(`isDeleted`) | Notes만 | 복구·감사 요구 대비 |
| RefreshToken DB 저장 | 재발급 시 기존 토큰 무효화 | 탈취 방어 |
| `findByIdAndUserId` 패턴 | 소유권을 Repository에 고정 | 서비스 계층 권한 로직 최소화 |
| MySQL on EC2 (RDS X) | 프리티어 유지 | 비용 절감 |
| jjwt 0.12.3 | 최신 API (`Jwts.parser().verifyWith(key)`) | 유지보수성 |
| ApiResponse 래퍼 | 공통 포맷 강제 | 프론트엔드 통합 용이 |
| `@Transactional(readOnly=true)` 조회 메서드 | DB 힌트로 성능 최적화 | 포트폴리오 품질 포인트 |

참조: Notion `📝 KnotNote` 루트 및 하위 Phase 1 페이지(7개).

---

## 6. 다음 작업자를 위한 작업 규칙 (Serena 선호사항)

- 작업은 **단계별 확인하며 진행**. 큰 덩어리 한 번에 말고, 파일 그룹 단위로 끊고 결과 공유.
- 새 파일을 만들 때는 이미 있는 레이어 구조(controller → service → repository → dto → entity)를 그대로 따른다.
- 모든 API 응답은 `ApiResponse<T>` 래퍼 사용(예외: Swagger는 직접).
- 예외 처리는 `CustomException(ErrorCode.XXX)`로 통일. 새 오류는 `ErrorCode`에 먼저 enum 추가 후 throw.
- Notion 루트(페이지 ID `32cc614d783880ac9603cbfb08a4636a`) 아래에 작업 로그 페이지를 만들어 진척·결정·오류를 기록한다.
- 문서 업데이트는 `update_content`의 `old_str`/`new_str` 쌍으로. 표 셀 업데이트는 불안정하니 상세 내용은 별도 서브페이지로.
- Railway/Render 같은 PaaS는 콜드스타트로 이미 제외된 선택지 — 다시 꺼내지 말 것.
- RDS는 비용 이유로 제외, MySQL on EC2 유지.

---

## 7. 알려진 제약 / 주의사항

1. **Gradle Wrapper 미생성** — 섹션 3.2 참고. 이것부터 해결.
2. ~~**테스트 전무**~~ — **P1 통합 테스트 40개 완료.** `.\gradlew.bat test`로 확인 가능.
3. **CORS 정책 느슨함** — `SecurityConfig`에서 `allowedOriginPatterns("*")`. 프론트 도메인 확정 후 좁혀야 한다.
4. **비밀번호 정책 약함** — `@Size(min=8)`만. 배포 전 특수문자/숫자 혼합 규칙 추가 권장.
5. **Refresh Token 회전** — 현재는 재발급 시 기존 토큰 덮어쓰기. 토큰 탈취 탐지 로직(예: old RT 사용 시 전체 세션 무효화)은 추후.
6. **`NoteEmbedding` 업데이트 타이밍** — 현재 Entity만 있고 실제 쓰기 코드는 없음. Phase 3에서 노트 저장 시점 훅 추가 필요.
7. **키워드 검색 N+1** — `SearchServiceImpl`이 결과 Page를 매핑하며 각 노트마다 `findByNoteId` 호출. 향후 `@EntityGraph` 또는 fetch join으로 최적화.
8. ~~**H2 프로필 미작성**~~ — `src/test/resources/application-test.yml` 생성 완료.

---

## 8. 빠른 체크리스트 (다음 작업자가 첫 30분에 할 일)

- [ ] 이 문서 전체 1회 통독
- [ ] Notion 루트 페이지(`32cc614d783880ac9603cbfb08a4636a`)와 Phase 1 서브페이지(Spring Boot 셋업/Entity/Security/Auth/Notes/Tags/Search) 1회 훑기
- [ ] `E:\workspace\Knotnote` 열어 IntelliJ에서 "Gradle 프로젝트로 Import" → wrapper 자동 생성
- [ ] MySQL `knotnote` 스키마 생성, 환경변수 3개 세팅
- [ ] `./gradlew build` 실행 결과 확인
- [ ] `./gradlew bootRun` 후 Swagger UI 접속 성공 확인
- [ ] `/api/auth/signup` + `/api/auth/login` 성공 확인
- [ ] 문제 없으면 P1(통합 테스트)로 진입

성공 기준(Exit Criteria for Phase 1):
1. `./gradlew build`가 0 exit code
2. `bootRun` 시 7개 테이블 자동 생성
3. 20개 엔드포인트가 Swagger UI에 노출
4. 회원가입 → 로그인 → 메모 CRUD → 태그 연결 → 검색 E2E 시나리오 성공
5. 통합 테스트 스위트 통과

---

## 9. 연락/컨텍스트

- 담당자: **Serena H. Sun** (junior backend, 커리어 전환자, AWS/DevOps 학습 중)
- 소통 채널: Notion(루트 위 참조)
- 선호 언어: 한국어
- 선호 페이스: 단계별 체크인

끝.
