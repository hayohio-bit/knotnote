# KnotNote Backend

AI가 메모를 연결하고 인사이트를 찾아주는 스마트 메모 서비스의 백엔드 프로젝트.

## Stack
- Java 17 / Spring Boot 3.2.5 / Gradle
- Spring Data JPA, Spring Security, jjwt 0.12.3
- MySQL 8 (local: H2 지원), Hibernate
- springdoc-openapi 2.3.0 (Swagger UI)

## Package
```
com.knotnote.backend
 ├── config       # Security, Swagger, JPA 설정
 ├── common       # ApiResponse 등 공통
 ├── controller   # REST 컨트롤러 (Auth, Notes, Tags, Search, Users)
 ├── service      # 비즈니스 로직
 ├── repository   # JPA Repository
 ├── entity       # JPA Entity (User, Note, Tag, NoteTag, NoteLink, NoteEmbedding, RefreshToken)
 ├── dto          # request / response DTO
 ├── security     # JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetailsService
 └── exception    # ErrorCode, CustomException, GlobalExceptionHandler
```

## Getting Started
1. MySQL 8 준비
   ```sql
   CREATE DATABASE knotnote CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'knotnote'@'localhost' IDENTIFIED BY 'yourpassword';
   GRANT ALL PRIVILEGES ON knotnote.* TO 'knotnote'@'localhost';
   FLUSH PRIVILEGES;
   ```
2. 환경변수 설정
   ```
   DB_USERNAME=knotnote
   DB_PASSWORD=yourpassword
   JWT_SECRET=<at-least-32-byte-random-string>
   ```
3. 실행
   ```
   ./gradlew bootRun
   ```
4. Swagger UI: http://localhost:8080/swagger-ui.html

## Domains & Endpoints
- `Auth` /api/auth/signup, /api/auth/login, /api/auth/refresh
- `Users` /api/users/me
- `Notes` /api/notes (CRUD, 페이징), /api/notes/{id}/links
- `Tags` /api/tags, /api/notes/{id}/tags
- `Search` /api/search?q=..., /api/search/semantic (Phase 3)

## Phase 로드맵
- Phase 1 (진행) 백엔드 구현
- Phase 2 프론트엔드 (React + Vite)
- Phase 3 AI 시맨틱 검색 (Python + OpenAI Embeddings)
- Phase 4 AWS 배포 (EC2 + S3 + CloudFront + GitHub Actions)
- Phase 5 포트폴리오 정리
