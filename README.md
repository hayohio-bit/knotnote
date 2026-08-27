# KnotNote

AI가 메모를 연결하고 인사이트를 찾아주는 스마트 메모 서비스.

## Stack
- Backend: Java 17+ / Spring Boot 3.2.5 / Gradle, Spring Data JPA, Spring Security, jjwt 0.12.3
- DB: MySQL 8 (로컬 개발은 H2 인메모리 대체 가능), Hibernate
- Frontend: React 18 + Vite (`frontend/`)
- AI: 임베딩 서버 (Python FastAPI, `embed_server/`) + Gemini API (OpenAI 호환 엔드포인트)
- Docs: springdoc-openapi 2.3.0 (Swagger UI)

## Package
```
com.knotnote.backend
 ├── config       # Security, Swagger, JPA 설정
 ├── common       # ApiResponse 등 공통
 ├── controller   # REST 컨트롤러 (Auth, Notes, Tags, Search, Users, Stats, Share, ...)
 ├── service      # 비즈니스 로직
 ├── repository   # JPA Repository
 ├── entity       # JPA Entity (User, Note, Tag, NoteTag, NoteLink, NoteEmbedding, RefreshToken)
 ├── dto          # request / response DTO
 ├── security     # JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetailsService
 └── exception    # ErrorCode, CustomException, GlobalExceptionHandler
```

## 실행 방법

### A. Docker Compose (전체 스택)
MySQL + 임베딩 서버 + 백엔드 + 프론트엔드 + Caddy를 한 번에 실행한다.
```bash
JWT_SECRET=<at-least-32-byte-random-string> \
OPENAI_API_KEY=<gemini-or-openai-key> \
docker compose up -d
```

### B. 로컬 개발 (MySQL)
1. MySQL 8에 데이터베이스·계정 생성:
   ```
   mysql -u root -p < db/init.sql
   ```
2. 최초 1회 `scripts\setup.ps1` 실행 (`.env.local` 생성 + 빌드)
3. 백엔드: `scripts\run.ps1` / 프론트엔드: `scripts\run-frontend.ps1`

### C. 로컬 개발 (H2 인메모리, MySQL 불필요)
데이터가 재시작 시 사라지는 대신 MySQL 없이 바로 실행된다.
```bash
SPRING_DATASOURCE_URL="jdbc:h2:mem:knotnote;MODE=MySQL;DATABASE_TO_LOWER=TRUE" \
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver \
SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.H2Dialect \
DB_USERNAME=sa DB_PASSWORD=sa \
JWT_SECRET=<at-least-32-byte-random-string> \
./gradlew bootRun
```

- 프론트엔드 개발 서버: http://localhost:3000 (API는 8080으로 프록시)
- Swagger UI: http://localhost:8080/swagger-ui.html
- `OPENAI_API_KEY` 미설정 시 AI 요약이, 임베딩 서버(8000) 미실행 시 시맨틱 검색이 비활성화되고 나머지 기능은 정상 동작한다.

## 화면

| 대시보드 | 지식 그래프 |
| --- | --- |
| ![대시보드](docs/screenshots/03-dashboard.jpg) | ![지식 그래프](docs/screenshots/06-graph.jpg) |

| 메모 편집기 | 통계 대시보드 |
| --- | --- |
| ![메모 편집기](docs/screenshots/05-editor.jpg) | ![통계 대시보드](docs/screenshots/08-stats.jpg) |

화면별 사용 방법은 **[사용 가이드](docs/USAGE.md)** 에 정리해 두었습니다.

## 빠른 사용법

1. 회원가입 후 로그인하면 대시보드로 이동합니다.
2. **+ 새 메모** 로 메모를 작성하고 오른쪽 사이드바에서 태그를 붙입니다.
3. 편집기의 **연결 추천** 에서 관련 있는 메모를 이어 둡니다.
4. **그래프** 메뉴에서 연결 구조와 각 메모의 활력(Knot Vitality)을 확인합니다.
5. **통계** 메뉴에서 축적 현황을 보고, Markdown 또는 JSON 으로 내보냅니다.

각 단계의 화면과 세부 옵션은 [사용 가이드](docs/USAGE.md) 를 참고해 주세요.

## Domains & Endpoints
- `Auth` /api/auth/signup, /api/auth/login, /api/auth/refresh
- `Users` /api/users/me
- `Notes` /api/notes (CRUD, 페이징), /api/notes/{id}/links, /api/notes/{id}/pin, /api/notes/graph
- `Tags` /api/tags, /api/notes/{id}/tags
- `Search` /api/search?q=..., /api/search/semantic
- `Stats` /api/stats, `Share` /api/notes/{id}/share → /shared/{token}

## Phase 로드맵
- Phase 1 백엔드 구현 (완료)
- Phase 2 프론트엔드 React + Vite (완료)
- Phase 3 AI 시맨틱 검색 — Python 임베딩 서버 + Gemini (완료)
- Phase 4 AWS 배포 (EC2 + S3 + CloudFront + GitHub Actions)
- Phase 5 포트폴리오 정리
