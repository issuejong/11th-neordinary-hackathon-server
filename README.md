# 11th-neordinary-hackathon-server

**NEORDINARY HACKATHON 8조 — 루틴 공유 서비스 iOS 앱 백엔드**

나만의 루틴(스텝 1~10개)을 만들어 공유하고, 다른 사람의 루틴을 **함께하기**로 따라 하며 결과를 남기는 서비스입니다.

| | |
| --- | --- |
| 운영 서버 | https://neo8.duckdns.org |
| API 명세 (Swagger) | https://neo8.duckdns.org/swagger-ui/index.html |
| ERD | [`docs/8조 erd.png`](docs/8조%20erd.png) · 상세 명세 [`ERD.md`](ERD.md) |
| 인프라 구성도 | [`docs/8조 인프라 구성도.png`](docs/8조%20인프라%20구성도.png) |

## 심사용 빠른 시작

1. Swagger에서 **Test Account › `POST /api/test-accounts/{publicId}/claim`** 을 예시값(8번 · 데모 유저) 그대로 실행
2. 응답의 `result.accessToken` 을 오른쪽 위 **Authorize** 에 입력
3. 모든 API를 **예시값 그대로** 실행하면 성공합니다
   - 수정 · 삭제 · 함께하기 예시 데이터는 데모 유저 소유이며, 삭제해도 **10초 안에 자동 복구**됩니다
   - 8번 심사용 계정은 사용 중이어도 다시 선택할 수 있습니다 (이전 토큰은 만료)

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| Language · Framework | Java 25, Spring Boot 4.1, Spring Data JPA, Bean Validation, springdoc-openapi 3 |
| Database | MySQL 8.4 (운영: Cloud SQL / 로컬: Docker), 테스트: H2 (MySQL 모드) |
| Infra | Google Cloud — Cloud Run, Cloud SQL, Global External Application LB, Secret Manager, Artifact Registry |
| CI/CD | GitHub Actions (Workload Identity Federation, 키 파일 없음) |

## 아키텍처

```
iOS 앱 ──HTTPS──▶ Cloud Load Balancing (neo8.duckdns.org, 고정 IP, Google 관리형 인증서, 80→443)
                      └─▶ Cloud Run · Spring Boot (LB 경유 요청만 허용)
                              ├─▶ Cloud SQL · MySQL 8.4 (Private IP만, 외부 접근 불가)
                              └─◀ Secret Manager (DB 접속 정보를 환경변수로 주입)
```

- **WAS / DB 분리**: WAS는 Cloud Run, DB는 관리형 Cloud SQL
- **보안**: 외부 공개 포트는 LB 80·443뿐, DB는 Public IP 없음, VPC 방화벽은 내부 통신만 허용, 비밀값은 코드·레포·GitHub에 없음
- 자세한 내용은 [인프라 구성도](docs/8조%20인프라%20구성도.png) 참고

## 프로젝트 구조

```
com.example.server
├── domain
│   ├── user      # 사용자 · 테스트 계정(사용권)
│   ├── image     # 이미지 업로드 · 조회 (DB MEDIUMBLOB 저장)
│   └── routine   # 루틴 · 스텝 · 좋아요 · 댓글 · 참여(함께하기)
│       ├── controller   # 요청 · 응답만
│       ├── service      # 비즈니스 로직 (command: 쓰기 / query: 읽기)
│       ├── repository   # DB 접근 (목록 · 상세는 projection 조회)
│       ├── converter    # 엔티티 ↔ DTO
│       ├── dto · entity · support
└── global
    ├── exception  # 전역 예외처리 (GlobalExceptionHandler)
    ├── response   # 공통 응답 ApiResponse · 성공/에러 코드
    ├── auth       # @LoginUser (Authorization: Bearer 토큰 → 사용자)
    ├── config     # Swagger · JPA · 웹 설정
    └── init       # Swagger 예시용 데모 데이터
```

## API

🔒 로그인 필요 · 🔓 로그인 선택(보내면 내 상태 표시) · 표시 없음은 공개

| 도메인 | 메서드 | 경로 | 설명 | |
| --- | --- | --- | --- | --- |
| Test Account | GET | `/api/test-accounts` | 테스트 계정 목록 | |
| | POST | `/api/test-accounts` | 테스트 계정 등록 | |
| | POST | `/api/test-accounts/{publicId}/claim` | 테스트 계정 선택 (사용권 발급) | |
| | DELETE | `/api/test-accounts/{publicId}/claim` | 테스트 계정 사용 종료 | 🔒 |
| | POST | `/api/test-accounts/{publicId}/heartbeat` | 사용 시간 연장 | 🔒 |
| User | GET | `/api/users/me` | 내 프로필 | 🔒 |
| | PATCH | `/api/users/me` | 내 프로필 수정 | 🔒 |
| | GET | `/api/users/{publicId}` | 공개 프로필 | |
| Image | POST | `/api/images` | 이미지 업로드 | 🔒 |
| | POST | `/api/images/profile` | 프로필 이미지 업로드 | 🔒 |
| | POST | `/api/images/step` | 스텝 이미지 업로드 | 🔒 |
| | GET | `/api/images/{imageId}` | 이미지 조회 (바이너리) | |
| Routine | GET | `/api/routines` | 루틴 목록 (메인 · 프로필 공용) | 🔓 |
| | POST | `/api/routines` | 루틴 생성 | 🔒 |
| | GET | `/api/routines/{routineId}` | 루틴 상세 | |
| | PUT | `/api/routines/{routineId}` | 루틴 수정 (작성자) | 🔒 |
| | DELETE | `/api/routines/{routineId}` | 루틴 삭제 (작성자) | 🔒 |
| | GET | `/api/routines/{routineId}/interactions` | 좋아요 · 참여 상태와 집계 | 🔒 |
| Like | PUT · DELETE | `/api/routines/{routineId}/like` | 좋아요 설정 · 취소 | 🔒 |
| Comment | GET | `/api/routines/{routineId}/comments` | 댓글 목록 | 🔓 |
| | POST | `/api/routines/{routineId}/comments` | 댓글 작성 | 🔒 |
| | PATCH · DELETE | `/api/routines/{routineId}/comments/{commentId}` | 댓글 수정 · 삭제 (작성자) | 🔒 |
| 함께하기 | PUT · DELETE | `/api/routines/{routineId}/participation` | 참여 시작 · 취소 | 🔒 |
| | GET · PUT | `/api/routines/{routineId}/participation/draft` | 내 초안 조회 · 저장 | 🔒 |
| | PUT | `/api/routines/{routineId}/participation/completion` | 참여 완료 (결과 제출) | 🔒 |
| 함께한 결과 | GET | `/api/routines/{routineId}/participation-results` | 공개된 결과 목록 | |
| | GET | `/api/routines/{routineId}/participation-results/{participantId}` | 결과 상세 | |
| | PUT · DELETE | `/api/routines/{routineId}/participation-results/{participantId}` | 내 결과 수정 · 삭제 | 🔒 |
| Health | GET | `/health` | 헬스 체크 | |

### 공통 응답 · 에러

```json
{ "isSuccess": false, "code": "COMMON400", "message": "요청 형식이 올바르지 않습니다.",
  "result": { "title": "루틴 제목은 5자 이상 30자 이하여야 합니다.", "cards[0].time": "시간은 필수입니다." } }
```

- 모든 응답은 `isSuccess` · `code` · `message` · `result` 형식이며, 성공은 `COMMON200`(조회 · 수정 · 삭제) / `COMMON201`(생성)
- 에러 코드의 숫자는 HTTP 상태와 같습니다 — `ROUTINE404` → 404, `IMAGE4003` → 400, `TEST401` → 401
- 모든 예외는 `GlobalExceptionHandler` 한 곳에서 처리합니다 (400 · 401 · 403 · 404 · 405 · 406 · 409 · 415 · 500). 입력값 오류(400)는 `result`에 **필드별 메시지**를 담습니다
- 전체 에러 코드는 Swagger 상단 **에러 코드 전체 보기** 참고

### 목록 · 프로필

```
메인          GET /api/routines?sort=latest|likes|participants&size=20
내 루틴 탭     GET /api/routines?authorId=<publicId>
참여 루틴 탭   GET /api/routines?participantId=<publicId>   (참여 중 + 완료, 취소 제외)
다음 페이지    ... &cursor=<응답의 nextCursor>               (hasNext=false면 끝)
```

- 응답 모양은 모두 같고, `totalCount`로 탭 개수를 표시합니다
- 좋아요순 · 참여순 커서는 첫 페이지에서 정한 순서를 15분간 유지합니다 (스크롤 중 순위 변동으로 인한 중복 · 누락 방지). 만료되면 커서 없이 다시 요청합니다

### 테스트 계정 (인증)

실제 로그인 대신 **고정 테스트 계정 8개**(1~7번 팀 테스트 계정, **8번 심사용 데모 유저**)를 선택해 사용권을 받습니다.

| 단계 | API | 설명 |
| --- | --- | --- |
| 목록 | `GET /api/test-accounts` | `available: false`인 계정은 다른 세션이 사용 중 |
| 선택 | `POST /api/test-accounts/{publicId}/claim` | `accessToken` 발급. 사용 중이면 409 (8번은 재발급) |
| 사용 | `Authorization: Bearer <accessToken>` | 만료되면 401 → 다시 선택 |
| 연장 | `POST .../heartbeat` | 사용 중 약 30초마다 호출 (사용권 2분) |
| 종료 | `DELETE .../claim` | 계정 선택 화면으로 돌아갈 때 |

- 등록 `POST /api/test-accounts`는 빈 슬롯(1~8)에만 가능합니다. 8번은 서버 시작 시 데모 유저로 예약되며, 현재 1~8번이 모두 사용 중이면 `409 TEST4092`가 정상 응답입니다

### 루틴 함께하기

1. 참여 시작 → 진행 중에는 본인만 보는 **초안**을 저장 (빈 스텝 위치는 `null`)
2. 완료 시 원본과 **같은 수의 스텝**과 제목 · 소개를 제출하면 **함께한 결과**로 공개
3. 결과는 원본 루틴의 함께한 결과 목록에 표시되며, 본인만 수정 · 삭제할 수 있습니다

- 본인 루틴에는 참여할 수 없습니다 (`403 PARTICIPATION403`)
- 이전 버전에서 결과 없이 완료된 참여는 진행 중으로 표시되며, 결과를 제출하면 완료됩니다

### 이미지

| 종류 | 형식 | 최대 |
| --- | --- | --- |
| 일반 (`/api/images`) | jpg · png · heic · heif · webp · gif | 10MB, 가로 · 세로 8192px |
| 프로필 · 스텝 (`/profile`, `/step`) | jpg · png · heic · heif | 5MB |

- 형식은 Content-Type이 아닌 **파일 시그니처**로 판별해 위장 파일을 거절합니다
- 오브젝트 스토리지 없이 DB(`images.data` MEDIUMBLOB)에 저장하고, 조회 응답은 30일 캐시합니다

## 로컬 개발

```bash
docker compose up -d      # 로컬 MySQL 8.4 (localhost:3306)
./gradlew bootRun         # 기본 프로필 dev (JDK 25 필요) → http://localhost:8080/swagger-ui/index.html
./gradlew test            # test 프로필 (H2)
```

| 항목 | 값 |
| --- | --- |
| Host / Port | `localhost` / `3306` |
| Database | `hackathon` |
| User / Password | `hackathon` / `hackathon` (로컬 전용) |

- 데이터 초기화: `docker compose down -v`
- 서버 시작 시 기본 이미지 · 일러스트 · 데모 사용자 · 데모 루틴 · 8번 심사용 계정이 자동으로 들어갑니다 (이미 있으면 내용만 맞춤)

### 환경별 설정

| 프로필 | 용도 | DB | 비밀값 |
| --- | --- | --- | --- |
| `dev` (기본) | 로컬 개발 | Docker MySQL | 로컬 기본값 |
| `prod` | Cloud Run | Cloud SQL (Private IP) | Secret Manager → 환경변수 `DB_URL` · `DB_USERNAME` · `DB_PASSWORD` |
| `test` | 자동 테스트 | H2 (MySQL 모드) | — |

정책 값은 코드에 하드코딩하지 않고 `application.yml`의 `app.*`에서 관리합니다 (환경변수로 덮어쓰기 가능).

| 설정 | 기본값 | 의미 |
| --- | --- | --- |
| `app.image.max-dimension` | 8192 | 이미지 가로 · 세로 최대 픽셀 |
| `app.image.profile-max-size` · `step-max-size` | 5MB | 프로필 · 스텝 이미지 최대 용량 |
| `app.image.cache-max-age` | 30d | 이미지 조회 캐시 |
| `app.routine.ranking-cursor-ttl` | 15m | 좋아요순 · 참여순 커서 유효 시간 |
| `app.test-accounts.lease-duration` | 2m | 테스트 계정 사용권 유효 시간 |
| `app.demo.restore-interval` | 10s | Swagger 예시 데이터 자동 복구 주기 |

## 테스트 · 검증

- `./gradlew test` — 경계값 · 예외 케이스 · 권한 · 커서 · Swagger 예시 실행까지 포함한 통합 테스트
- `scripts/smoke-api.sh [BASE_URL]` — 실제 서버 대상 API 스모크 테스트 (테스트 계정을 선택해 실행 후 사용권 해제)

```bash
scripts/smoke-api.sh http://localhost:8080
```

## CI/CD · 브랜치

| 브랜치 | 동작 |
| --- | --- |
| 작업 브랜치 → `develop` | PR · push 시 빌드 · 테스트 (CI) |
| `develop` → `main` | 테스트 → Docker 빌드 → Artifact Registry → **Cloud Run 자동 배포** → 도메인 `/health` 스모크 테스트 |

- GitHub Actions는 Workload Identity Federation으로 인증해 키 파일이 없으며, `main` 브랜치에서만 배포 권한을 가집니다
- `*.md` 변경만 있는 push는 배포하지 않습니다
