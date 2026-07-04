/**
 * KnotNote 더미데이터 시드 스크립트
 * 실행: node seed.mjs
 * (백엔드가 localhost:8080 에서 실행 중이어야 합니다)
 */

const BASE = 'http://localhost:8080/api'

// ── 헬퍼 ──────────────────────────────────────────────────────
async function req(method, path, body, token) {
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  })
  const json = await res.json()
  if (!json.success) throw new Error(`${method} ${path} 실패: ${json.message}`)
  return json.data
}

// ── 더미 데이터 정의 ───────────────────────────────────────────
const TAGS = ['React', 'Spring Boot', 'Java', 'DevOps', '기획', 'AI', '독서', 'MySQL', '알고리즘', '커리어', 'Python', '회고']

const NOTES = [
  {
    title: 'React 상태관리 패턴 비교',
    content: `# React 상태관리 패턴 비교

## 1. useState / useReducer
컴포넌트 로컬 상태. 단순한 UI 상태에 적합.

## 2. Context API
전역 상태가 필요하지만 업데이트 빈도가 낮을 때 사용.
단점: 렌더링 최적화가 까다로움.

## 3. Zustand
경량 전역 상태 관리 라이브러리. Redux보다 보일러플레이트가 적음.
\`\`\`js
const useStore = create((set) => ({
  count: 0,
  inc: () => set((s) => ({ count: s.count + 1 })),
}))
\`\`\`

## 4. Redux Toolkit
대규모 앱에 적합. 미들웨어, DevTools 지원이 강점.

## 결론
소규모: Zustand / 대규모 팀: Redux Toolkit`,
    tags: ['React'],
  },
  {
    title: 'Spring Boot JWT 인증 구현 정리',
    content: `# Spring Boot JWT 인증 구현

## 핵심 컴포넌트
- JwtTokenProvider: 토큰 생성 / 검증
- JwtAuthenticationFilter: 요청마다 토큰 파싱
- SecurityConfig: 필터 체인 등록

## 토큰 전략
- Access Token: 1시간 (메모리 보관)
- Refresh Token: 14일 (DB 저장, 재발급 시 교체)

## 주의사항
jjwt 0.12.x 부터 API가 바뀜.
\`\`\`java
Jwts.parser()
    .verifyWith(key)
    .build()
    .parseSignedClaims(token)
\`\`\`

KnotNote 프로젝트에 적용 완료.`,
    tags: ['Spring Boot', 'Java'],
  },
  {
    title: 'Docker로 MySQL 로컬 환경 구성',
    content: `# Docker MySQL 로컬 환경 설정

## docker-compose.yml
\`\`\`yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: knotnote
      MYSQL_USER: knotnote
      MYSQL_PASSWORD: knotnote1234!
    ports:
      - "3306:3306"
\`\`\`

## 실행
\`\`\`bash
docker compose up -d
\`\`\`

## 주의
MySQL 8.0 + allowPublicKeyRetrieval=true 설정 필수.
JDBC URL: jdbc:mysql://localhost:3306/knotnote?allowPublicKeyRetrieval=true&useSSL=false`,
    tags: ['DevOps', 'Spring Boot'],
  },
  {
    title: 'KnotNote 프로젝트 기획 메모',
    content: `# KnotNote 기획

## 핵심 가치
"메모를 연결하면 인사이트가 보인다"

## 타겟 유저
- 지식 노동자, 개발자, 기획자
- 많은 메모를 쓰지만 나중에 못 찾는 사람

## MVP 기능
1. 메모 CRUD
2. 태그 분류
3. 양방향 링크
4. 키워드 검색
5. JWT 인증

## Phase 3 목표
- AI 시맨틱 검색 (text-embedding-3-small)
- 연관 메모 자동 추천
- AI 태그 추천

## 포트폴리오 차별점
7년 비즈니스 경험 + 풀스택 + AI 기능`,
    tags: ['기획', 'AI'],
  },
  {
    title: '독서 노트: 함께 자라기',
    content: `# 함께 자라기 — 애자일로 가는 길 (김창준)

## 핵심 메시지
혼자 잘하는 것보다 함께 잘하는 것이 더 중요하다.
애자일은 방법론이 아니라 학습하는 문화다.

## 인상 깊은 구절
"실력은 고정된 것이 아니라 학습을 통해 계속 성장한다."

## 적용 포인트
- 일일 회고 습관화
- 코드 리뷰를 학습의 기회로
- 빠른 피드백 루프 만들기

## 평점: ★★★★★`,
    tags: ['독서'],
  },
  {
    title: 'AWS 배포 체크리스트',
    content: `# AWS 배포 체크리스트

## EC2 설정
- [ ] t2.micro 인스턴스 생성
- [ ] 보안그룹: 22(SSH), 8080(API), 80, 443
- [ ] Java 21 설치
- [ ] MySQL 8.0 설치

## S3 + CloudFront
- [ ] S3 버킷 생성 (정적 호스팅)
- [ ] CloudFront 배포
- [ ] ACM 인증서 발급 (HTTPS)

## GitHub Actions CI/CD
- [ ] deploy-backend.yml
- [ ] deploy-frontend.yml
- [ ] Secrets 등록: JWT_SECRET, DB_PASSWORD 등

## 예상 비용
EC2 t2.micro + S3 + CloudFront = 프리티어 범위 내 무료`,
    tags: ['DevOps', 'Spring Boot'],
  },
  {
    title: 'Vite + React 프로젝트 세팅 팁',
    content: `# Vite + React 세팅 팁

## 초기 세팅
\`\`\`bash
npm create vite@latest my-app -- --template react
cd my-app && npm install
\`\`\`

## 유용한 플러그인
- @vitejs/plugin-react: HMR 지원
- vite-plugin-svgr: SVG를 컴포넌트로

## 개발 서버 프록시 설정
\`\`\`js
// vite.config.js
server: {
  proxy: {
    '/api': 'http://localhost:8080'
  }
}
\`\`\`

이렇게 하면 CORS 없이 백엔드 API 호출 가능.`,
    tags: ['React'],
  },

  // ── 추가 더미 메모 ──────────────────────────────────────────
  {
    title: 'MySQL 인덱스 설계 원칙',
    content: `# MySQL 인덱스 설계 원칙

## 인덱스를 걸어야 할 때
- WHERE 절에 자주 등장하는 컬럼
- JOIN ON 조건에 쓰이는 컬럼
- ORDER BY, GROUP BY 대상 컬럼

## 복합 인덱스 순서
선택도(Cardinality)가 높은 컬럼을 앞에 배치.
예: (user_id, created_at) — user_id가 선택도 높음.

## 인덱스가 오히려 독이 되는 경우
- 전체 행의 20% 이상을 스캔하는 쿼리
- INSERT/UPDATE/DELETE가 매우 잦은 테이블

## EXPLAIN으로 확인하기
\`\`\`sql
EXPLAIN SELECT * FROM notes
WHERE user_id = 1 AND is_deleted = 0
ORDER BY created_at DESC;
\`\`\`
type: range 이상이면 인덱스 활용 중.`,
    tags: ['MySQL', 'Java'],
  },
  {
    title: 'JPA N+1 문제와 해결법',
    content: `# JPA N+1 문제

## 문제 상황
OneToMany 관계에서 부모 엔티티 목록 조회 시
자식 엔티티를 N번 추가 쿼리로 조회하는 현상.

## 해결법 1: @EntityGraph
\`\`\`java
@EntityGraph(attributePaths = {"tags"})
List<Note> findByUserId(Long userId);
\`\`\`

## 해결법 2: Fetch Join
\`\`\`java
@Query("SELECT n FROM Note n JOIN FETCH n.tags WHERE n.user.id = :userId")
List<Note> findWithTags(@Param("userId") Long userId);
\`\`\`

## 해결법 3: Batch Size
\`\`\`yaml
spring.jpa.properties.hibernate.default_batch_fetch_size: 100
\`\`\`

## KnotNote 적용 현황
현재 NoteTag를 별도 Repository로 조회 중 → 향후 Fetch Join으로 최적화 예정.`,
    tags: ['Java', 'Spring Boot', 'MySQL'],
  },
  {
    title: 'Python으로 텍스트 임베딩 구현',
    content: `# OpenAI 텍스트 임베딩

## 목적
KnotNote Phase 3 — 시맨틱 검색을 위한 벡터 생성.

## 설치
\`\`\`bash
pip install openai numpy
\`\`\`

## 임베딩 생성
\`\`\`python
from openai import OpenAI

client = OpenAI(api_key="sk-...")

def get_embedding(text: str) -> list[float]:
    response = client.embeddings.create(
        model="text-embedding-3-small",
        input=text
    )
    return response.data[0].embedding
\`\`\`

## 코사인 유사도
\`\`\`python
import numpy as np

def cosine_similarity(a, b):
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))
\`\`\`

## 비용
text-embedding-3-small: $0.02 / 1M tokens — 매우 저렴.`,
    tags: ['Python', 'AI'],
  },
  {
    title: '이진 탐색 완전 정리',
    content: `# 이진 탐색 (Binary Search)

## 핵심 조건
배열이 정렬되어 있어야 한다.

## 기본 구현
\`\`\`java
int binarySearch(int[] arr, int target) {
    int left = 0, right = arr.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (arr[mid] == target) return mid;
        if (arr[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return -1;
}
\`\`\`

## Lower Bound (같거나 큰 첫 인덱스)
\`\`\`java
int lowerBound(int[] arr, int target) {
    int left = 0, right = arr.length;
    while (left < right) {
        int mid = (left + right) / 2;
        if (arr[mid] < target) left = mid + 1;
        else right = mid;
    }
    return left;
}
\`\`\`

## 시간복잡도
O(log N) — N=10억이어도 30번이면 끝.`,
    tags: ['알고리즘', 'Java'],
  },
  {
    title: '커리어 전환 회고 — 비즈니스 → 개발',
    content: `# 커리어 전환 회고

## 배경
7년간 비즈니스/기획 직무 → 백엔드 개발자 전환 도전 중.

## 잘한 것
- 포트폴리오 프로젝트(KnotNote) 직접 기획부터 개발
- 비즈니스 경험을 기술 스토리에 녹이는 전략
- 매일 커밋 습관 유지

## 어려웠던 것
- 자바/스프링 학습 곡선이 생각보다 가팔랐음
- 알고리즘 문제풀이에 많은 시간 소요
- 포트폴리오와 취업 준비를 병행하는 체력 관리

## 앞으로 할 것
1. KnotNote AWS 배포 완료
2. 기술 블로그 포스팅 3편
3. 코딩테스트 매일 1문제

## 마음가짐
늦게 시작했지만, 비즈니스 감각은 나만의 무기다.`,
    tags: ['커리어', '회고'],
  },
  {
    title: 'HTTP 메서드 & REST 설계 원칙',
    content: `# REST API 설계 원칙

## HTTP 메서드 정의
| 메서드 | 의미 | 멱등성 |
|--------|------|--------|
| GET | 조회 | O |
| POST | 생성 | X |
| PUT | 전체 수정 | O |
| PATCH | 부분 수정 | △ |
| DELETE | 삭제 | O |

## URL 설계 규칙
- 명사 사용: /notes (O), /getNotes (X)
- 계층 표현: /notes/{id}/tags
- 복수형: /notes (O), /note (X)

## 상태코드
- 200 OK, 201 Created, 204 No Content
- 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found
- 409 Conflict, 500 Internal Server Error

## KnotNote 적용
20개 엔드포인트 모두 위 원칙 준수.`,
    tags: ['Spring Boot', '기획'],
  },
  {
    title: '2026년 4월 회고',
    content: `# 2026년 4월 회고

## 이번 달 한 일
- KnotNote 백엔드 완성 (Spring Boot + MySQL)
- 통합 테스트 40개 작성
- 프론트엔드 React 개발 시작
- Docker로 로컬 환경 구성

## 잘된 것 ✅
- 계획한 기능 모두 구현
- 테스트 코드 습관화
- 매일 커밋 유지

## 아쉬운 것 ❌
- 알고리즘 공부 소홀
- 블로그 포스팅 0편

## 5월 목표
- AWS 배포 완료
- 기술 블로그 2편 작성
- 코딩테스트 준비 본격 시작

## 한 줄 평
"만드는 것의 즐거움을 다시 느꼈다."`,
    tags: ['회고', '커리어'],
  },
  {
    title: 'Git 브랜치 전략 정리',
    content: `# Git 브랜치 전략

## GitHub Flow (현재 사용 중)
main ← feature/xxx

단순하고 빠른 배포에 적합. 소규모 팀에 추천.

## Git Flow
main ← develop ← feature/xxx
             ↑
          release/x.x
             ↑
           hotfix/xxx

복잡하지만 버전 관리에 강점.

## 커밋 메시지 컨벤션
\`\`\`
feat: 메모 검색 기능 추가
fix: JWT 만료 시 500 오류 수정
refactor: NoteService 메서드 분리
test: TagIntegrationTest 추가
docs: README 업데이트
\`\`\`

## KnotNote 운영 방식
feature/ 브랜치 → PR → main 머지 → 자동 배포 (예정).`,
    tags: ['DevOps', '기획'],
  },
  {
    title: 'AI 프롬프트 엔지니어링 기초',
    content: `# 프롬프트 엔지니어링

## 핵심 원칙
1. 명확하고 구체적으로 작성
2. 역할을 부여 ("당신은 시니어 개발자입니다")
3. 예시를 포함 (Few-shot)
4. 단계적으로 생각하게 유도 (Chain of Thought)

## 좋은 예시
"Spring Boot로 JWT 인증 필터를 구현해줘.
OncePerRequestFilter를 상속하고,
Authorization 헤더에서 Bearer 토큰을 추출해서
JwtTokenProvider로 검증하는 방식으로."

## 나쁜 예시
"JWT 인증 만들어줘."

## KnotNote에 활용
- AI 태그 자동 추천 프롬프트 설계 (Phase 3)
- 연관 메모 추천 알고리즘 설계에 활용`,
    tags: ['AI', '기획'],
  },
]

// 링크 연결 정의 (인덱스 기반)
const LINKS = [
  [0,  1],  // React 상태관리 ↔ JWT 인증
  [1,  2],  // JWT 인증 ↔ Docker MySQL
  [1,  5],  // JWT 인증 ↔ AWS 배포
  [2,  5],  // Docker ↔ AWS 배포
  [3,  4],  // 기획 ↔ 독서 노트
  [3,  6],  // 기획 ↔ Vite 팁
  [0,  6],  // React 상태관리 ↔ Vite 팁
  [7,  8],  // MySQL 인덱스 ↔ JPA N+1
  [8,  1],  // JPA N+1 ↔ JWT 인증
  [7,  2],  // MySQL 인덱스 ↔ Docker MySQL
  [9, 12],  // Python 임베딩 ↔ AI 프롬프트
  [9,  3],  // Python 임베딩 ↔ KnotNote 기획
  [10, 8],  // 이진탐색 ↔ JPA N+1
  [11, 15], // 커리어 회고 ↔ 4월 회고
  [11,  3], // 커리어 회고 ↔ KnotNote 기획
  [13, 14], // HTTP REST ↔ Git 브랜치
  [13,  3], // HTTP REST ↔ KnotNote 기획
  [14,  5], // Git 브랜치 ↔ AWS 배포
  [12,  3], // AI 프롬프트 ↔ KnotNote 기획
  [15, 11], // 4월 회고 ↔ 커리어 회고 (이미 있으므로 중복 처리됨)
]

// ── 실행 ──────────────────────────────────────────────────────
async function seed() {
  console.log('🌱 KnotNote 더미데이터 시드 시작...\n')

  // 1. 로그인
  console.log('1️⃣  로그인 중...')
  let token
  try {
    const auth = await req('POST', '/auth/login', {
      email: 'test@test.com',
      password: 'password1!',
    })
    token = auth.accessToken
    console.log('   ✅ 로그인 성공\n')
  } catch (e) {
    // 계정 없으면 회원가입 후 로그인
    console.log('   계정 없음, 회원가입 진행...')
    await req('POST', '/auth/signup', {
      email: 'test@test.com',
      password: 'password1!',
      nickname: 'Serena',
    })
    const auth = await req('POST', '/auth/login', {
      email: 'test@test.com',
      password: 'password1!',
    })
    token = auth.accessToken
    console.log('   ✅ 회원가입 + 로그인 성공\n')
  }

  // 2. 태그 생성
  console.log('2️⃣  태그 생성 중...')
  const tagMap = {}
  for (const name of TAGS) {
    try {
      const tag = await req('POST', '/tags', { name }, token)
      tagMap[name] = tag.id
      console.log(`   ✅ #${name}`)
    } catch {
      // 이미 존재하면 목록에서 찾기
      const tags = await req('GET', '/tags', null, token)
      const existing = tags.find((t) => t.name === name)
      if (existing) tagMap[name] = existing.id
    }
  }
  console.log()

  // 3. 메모 생성
  console.log('3️⃣  메모 생성 중...')
  const noteIds = []
  for (const note of NOTES) {
    const created = await req('POST', '/notes', {
      title: note.title,
      content: note.content,
    }, token)
    noteIds.push(created.id)
    console.log(`   ✅ "${note.title}"`)

    // 태그 연결
    for (const tagName of note.tags) {
      if (tagMap[tagName]) {
        try {
          await req('POST', `/notes/${created.id}/tags?tagId=${tagMap[tagName]}`, null, token)
        } catch { /* 중복 무시 */ }
      }
    }
  }
  console.log()

  // 4. 메모 링크 연결
  console.log('4️⃣  메모 링크 연결 중...')
  for (const [a, b] of LINKS) {
    try {
      await req('POST', `/notes/${noteIds[a]}/links`, { targetNoteId: noteIds[b] }, token)
      console.log(`   ✅ "${NOTES[a].title}" ↔ "${NOTES[b].title}"`)
    } catch { /* 중복 무시 */ }
  }

  console.log('\n🎉 시드 완료! http://localhost:3000/dashboard 에서 확인하세요.')
}

seed().catch((e) => {
  console.error('❌ 오류:', e.message)
  process.exit(1)
})
