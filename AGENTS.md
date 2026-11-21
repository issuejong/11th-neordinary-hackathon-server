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
