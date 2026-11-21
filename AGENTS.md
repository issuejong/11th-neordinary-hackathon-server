# AGENTS.md

NEORDINARY HACKATHON 8조의 iOS 앱 백엔드. 루틴 공유 서비스입니다.
Claude Code(`CLAUDE.md`)와 Codex(`AGENTS.md`)가 이 파일 하나를 같이 씁니다. 규칙을 바꿀 때는 이 파일만 고치세요.

## 스택
- Java 25 (Gradle toolchain), Spring Boot 4.1, Spring Data JPA, Validation, springdoc-openapi 3, Lombok
- DB: MySQL 8.4 (로컬은 docker compose, 운영은 Cloud SQL). 테스트는 H2(MySQL 모드)
- 클라이언트가 iOS 앱이라 CORS 설정은 필요 없습니다.

## 명령어
```bash
docker compose up -d          # 로컬 MySQL (localhost:3306, hackathon/hackathon)
./gradlew bootRun             # dev 프로필(기본값)로 실행 → http://localhost:8080/swagger-ui/index.html
./gradlew test                # test 프로필(H2) — 커밋 전에 반드시 통과시킬 것
./gradlew build               # CI(develop push / PR)에서 도는 것과 동일
```
JDK 25가 PATH에 없다면 `JAVA_HOME`을 JDK 25로 지정해서 실행하세요.

## 브랜치 · 배포
- 개발은 `develop`에서 합니다. `main`은 배포 브랜치입니다.
- **`main`에 push(머지)하면 GitHub Actions가 테스트 → Docker 빌드 → Cloud Run에 자동 배포합니다.** 팀원과 합의 없이 `main`에 push하지 마세요.
- 커밋 메시지: `feat:`, `fix:`, `chore:`, `docs:`, `refactor:` + 한국어 설명. 예: `feat: 루틴 카드 생성 API 추가`
- 운영 주소: https://neo8.duckdns.org (Swagger: `/swagger-ui/index.html`, 헬스체크: `/health`)

## 패키지 구조
```
com.example.server
├── global/            # 공통: config, exception(GlobalExceptionHandler, BusinessException),
│                      #       response(ApiResponse, SuccessStatus/ErrorStatus), entity(Base/ActorAudited/SoftDeletable)
└── domain/<도메인>/    # user, image, routine, example(참고용 샘플)
    ├── controller/
    ├── dto/            # XxxRequestDTO(내부 record로 Create 등), XxxResponseDTO
    ├── converter/      # Entity ↔ DTO 변환
    ├── entity/
    ├── repository/
    └── service/command/ · service/query/   # 인터페이스 + Impl, 쓰기/읽기 분리
```
새 API는 `domain/example`을 그대로 따라 만드세요.

## 구현된 API (Swagger 예시값으로 바로 실행 가능)
| 메서드 | 경로 | 설명 | 인증 헤더 |
| --- | --- | --- | --- |
| POST | `/api/v1/images` | 이미지 업로드 (multipart `file`, jpeg/png/heic/heif/webp/gif, 10MB) → 201 | O |
| GET | `/api/v1/images/{imageId}` | 이미지 바이너리 조회 | — |
| POST | `/api/v1/routines` | 루틴 생성 (카드 1~10장) → 201 | O |
| GET | `/api/v1/routines/{routineId}` | 루틴 상세 (작성자, 카드, 좋아요·댓글 수) | — |
| PUT | `/api/v1/routines/{routineId}` | 루틴 수정 (카드 전체 교체), 작성자만 | O |
| DELETE | `/api/v1/routines/{routineId}` | 루틴 소프트 삭제, 작성자만 | O |

- **임시 인증**: 컨트롤러는 `@LoginUser Long userId`로 사용자를 받고, 지금은 `X-User-Id` 헤더(users.public_id)로 해석합니다(`global/auth/LoginUserArgumentResolver`). 유저 도메인(가입/로그인)이 완성되면 **리졸버만 교체**하고 컨트롤러는 그대로 둡니다. Swagger에는 헤더 입력칸이 자동으로 추가됩니다.
- **데모 데이터**: `global/init/DemoDataInitializer`가 시작할 때 기본 이미지 3개, 데모 유저, 루틴 2개를 고정 publicId(`DemoData`)로 넣습니다(이미 있으면 건너뜀). Swagger 예시값이 이 ID를 가리키므로 지우거나 바꾸지 마세요.
- ID는 `global/util/Ulid.generate()`로 만들고, 경로 변수는 `@Pattern(regexp = Ulid.REGEX)`로 검증합니다(형식 오류는 400).
- 목록·상세 조회에서 이미지 바이너리(MEDIUMBLOB)를 읽지 않도록 projection(`select new ...View(...)`)으로 조회합니다.

## 코드 규칙
- **응답**: 모든 API는 `ApiResponse<T>`로 감쌉니다. 성공은 `ApiResponse.onSuccess(result)`, 생성은 `ResponseEntity.created(...)` + `ApiResponse.of(SuccessStatus._CREATED, result)`.
- **에러**: `throw new BusinessException(ErrorStatus.XXX)`. 새 에러는 `ErrorStatus` enum에 `HttpStatus`, 코드(`DOMAIN4xx`), 한국어 메시지로 추가합니다.
- **URL**: `/api/v1/...`. 경로와 응답에는 `id`(BIGINT PK)를 노출하지 않고 `publicId`(ULID, `CHAR(26)`)만 씁니다.
- **엔티티**:
  - 스키마의 기준은 `ERD.md`입니다. 엔티티를 바꾸면 `ERD.md`도 같이 고치세요.
  - `@NoArgsConstructor(access = PROTECTED)`, `@Getter`만 쓰고 `@Setter`는 쓰지 않습니다. 값을 바꿀 때는 의도가 드러나는 메서드를 둡니다.
  - 연관관계는 `FetchType.LAZY`로 둡니다. 유니크 키와 인덱스 이름은 `uk_<테이블>_<컬럼>`, `idx_<테이블>_<용도>` 형식을 따릅니다.
  - 생성·수정 시각은 `BaseEntity`, 작업자 기록은 `ActorAuditedEntity`, 소프트 삭제는 `SoftDeletableEntity`(`markDeleted`)를 상속해서 씁니다.
- **이미지**: 오브젝트 스토리지 없이 DB `images.data`(MEDIUMBLOB, 최대 16MB)에 저장합니다. 업로드 한도는 파일 10MB, 요청 20MB입니다(`application.yml`). 목록 조회에서는 바이너리를 빼고 내려줍니다.
- **트랜잭션**: command 서비스에는 `@Transactional`, query 서비스에는 `@Transactional(readOnly = true)`를 붙입니다. `open-in-view`는 꺼져 있습니다.

## 심사 기준 (모든 작업은 이 기준을 통과해야 합니다)
심사자는 배포 URL(https://neo8.duckdns.org)의 Swagger에서 직접 API를 호출해서 채점합니다. **500 응답이 한 번이라도 나오면 감점**입니다.

**API 정상 동작 (35점)**
- 명세(Swagger)에 노출된 API는 전부 운영 서버에서 실제로 동작해야 합니다. 미완성 API는 Swagger에 노출하지 마세요.
- Swagger에서 바로 실행할 수 있도록 예시를 채웁니다.
  - 요청 DTO의 모든 필드에 `@Schema(description = "...", example = "...")`를 붙입니다.
  - 경로 변수와 쿼리 파라미터에는 `@Parameter(example = "...")`를 붙입니다.
  - 컨트롤러에는 `@Tag`, 메서드에는 `@Operation(summary = "...")`을 붙입니다.
- `publicId` 예시는 운영 DB에 실제로 있는 값을 쓰거나, 시드 데이터로 넣어서 바로 호출해도 성공하게 만듭니다.

**API 예외처리 (20점)**: 심사자가 쿼리 스트링과 요청 본문에 잘못된 값을 넣어 봅니다.
- 모든 예외는 `GlobalExceptionHandler`(`@RestControllerAdvice`)가 `ApiResponse.onFailure` 형식으로 응답합니다. 컨트롤러에서 try-catch로 직접 응답을 만들지 마세요.
- 상태 코드를 정확히 맞춥니다.

  | 상태 | 상황 |
  | --- | --- |
  | 200 | 조회 · 수정 · 삭제 성공 |
  | 201 | 생성 성공 (`ResponseEntity.created`) |
  | 400 | 검증 실패, 타입 불일치, 필수 파라미터 누락, JSON 파싱 실패, 파일 크기 초과 |
  | 404 | 없는 리소스 (`publicId` 조회 실패 포함) → `BusinessException(ErrorStatus.XXX_NOT_FOUND)` |
  | 500 | 예상하지 못한 예외만 (catch-all 핸들러). 스택 트레이스나 내부 메시지는 응답에 넣지 않습니다. |

- 입력 검증은 요청 DTO에 Bean Validation(`@NotBlank`, `@Size`, `@Min`, `@Pattern` 등)과 `@Valid`로 합니다. 쿼리 파라미터 검증이 필요하면 컨트롤러에 제약 어노테이션을 붙입니다.
- `Optional.get()`, `orElseThrow()`(인자 없음), null 역참조처럼 500이 날 수 있는 코드를 쓰지 마세요. 항상 `orElseThrow(() -> new BusinessException(...))`를 씁니다.
- 새 API를 만들면 정상 요청, 잘못된 본문(400), 없는 ID(404)를 테스트(MockMvc)로 확인합니다.

**코드 품질 (15점)**
- 관심사 분리: controller는 요청과 응답만, 비즈니스 로직은 service, 변환은 converter, DB 접근은 repository가 맡습니다. 컨트롤러에서 repository를 직접 호출하지 마세요. 엔티티를 응답으로 그대로 반환하지도 마세요(DTO로 변환).
- 중복 제거: 공통 로직은 `global/`에 둡니다. 응답 형식, 예외, 감사 필드(Base 엔티티)를 재사용하고 같은 코드를 복사해 붙이지 않습니다.
- 설정 외부화: URL, 비밀번호, 한도값 같은 환경별 값은 하드코딩하지 않고 `application.yml`과 환경변수(`${...}`)로 뺍니다.

## 설정 · 프로필 (`src/main/resources/application.yml`)
- `dev`(기본값): 로컬 MySQL, `ddl-auto: update`, SQL 로그 출력
- `prod`: Cloud Run에서 `SPRING_PROFILES_ACTIVE=prod`. `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`는 GCP Secret Manager에서 주입됩니다. `ddl-auto: update`라서 **배포하는 순간 운영 DB 스키마가 바뀝니다.** 컬럼 삭제나 타입 변경은 자동으로 반영되지 않습니다.
- `test`: `src/test/resources/application-test.yml` (H2, `create-drop`)

## 절대 하지 말 것
- 비밀번호, 키, `.env` 같은 시크릿을 커밋하지 마세요. 이 레포는 **public**입니다. 운영 값은 Secret Manager에만 있습니다.
- `docs/security/`(콘솔 캡처 등 보안 증빙)는 gitignore 대상입니다. 강제로 add하지 마세요.
- `.github/workflows/`, `Dockerfile`, GCP 인프라(Cloud Run, Cloud SQL, LB, 방화벽)는 인프라 담당과 상의한 뒤에만 수정하세요. 운영 DB는 Private IP만 열려 있어서 로컬에서 직접 접속할 수 없습니다.

## 인프라 요약
iOS 앱 → HTTPS LB(`neo8.duckdns.org`, Google 관리형 인증서, 80→443 리다이렉트) → Cloud Run `hackathon-server`(asia-northeast3, LB 경유 트래픽만 허용) → Direct VPC egress → Cloud SQL MySQL(Private IP). 구성도는 `docs/infra-architecture.png`에 있습니다.
