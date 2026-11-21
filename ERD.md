# 루틴 서비스 ERD 명세

공유된 스키마 기준으로 7개 테이블의 컬럼, 인덱스, 외래 키를 정리했습니다. 컬럼 타입은 MySQL 기준입니다.

## 테이블 개요

| 테이블 | 역할 |
| --- | --- |
| `users` | 사용자 프로필과 로그인 조회용 식별자를 저장합니다. |
| `images` | 기본 프로필 이미지와 카드 이미지의 바이너리를 저장합니다. |
| `routines` | 루틴의 소유자, 제목, 소개와 생성·수정·삭제 기록을 저장합니다. |
| `routine_cards` | 루틴을 구성하는 카드의 내용, 이미지, 시간과 순서를 저장합니다. |
| `routine_comments` | 루틴에 달린 댓글과 작성·수정·삭제 기록을 저장합니다. |
| `routine_likes` | 사용자별 루틴 좋아요 상태를 저장합니다. |
| `routine_participations` | 사용자별 루틴 참여 상태와 참여·완료 시각을 저장합니다. |

`PK`는 기본 키, `FK`는 외래 키, `AI`는 자동 증가를 뜻합니다. `created_by`, `updated_by`, `deleted_by`는 해당 작업을 수행한 사용자의 ID입니다.

## `users`

가입 시 기본 대표 이미지 중 하나를 무작위로 배정합니다.

### 컬럼

| 컬럼 | 타입 | NULL 허용 | 키 | 기본값 | 의미 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT` | NO | PK, AI | — | 내부 식별자(PK, 외부 노출 금지) |
| `public_id` | `CHAR(26)` | NO | — | — | 외부 노출용 식별자(ULID) |
| `device_token` | `VARCHAR(255)` | NO | — | — | 프론트가 전달하는 기기 개별 토큰(로그인 조회용, 참조 키로 사용 금지) |
| `nickname` | `VARCHAR(30)` | NO | — | — | 표시 이름. 프로필에서 변경할 때 2~10자를 허용합니다. |
| `nickname_key` | `VARCHAR(60)` | YES | — | — | 생성하거나 변경한 닉네임의 소문자 값입니다. 동시 요청의 중복을 방지합니다. 기존 데이터는 `NULL`을 유지하고 중복 검사에 `nickname`도 사용합니다. |
| `bio` | `VARCHAR(100)` | YES | — | — | 마이페이지 한 줄 소개. 프로필 수정 시 최대 50자이며 `null`로 지울 수 있습니다. |
| `profile_image_id` | `BIGINT` | NO | FK | — | 대표 이미지 images.id(가입 시 기본 이미지 중 랜덤 부여) |
| `created_at` | `DATETIME(6)` | NO | — | `CURRENT_TIMESTAMP(6)` | 생성일시 |
| `created_by` | `BIGINT` | YES | — | — | 생성 주체 user id(가입/시스템은 NULL) |
| `updated_at` | `DATETIME(6)` | NO | — | `CURRENT_TIMESTAMP(6)` | 수정일시 |
| `updated_by` | `BIGINT` | YES | — | — | 수정 주체 user id |
| `deleted_at` | `DATETIME(6)` | YES | — | — | 삭제일시(soft delete) |
| `deleted_by` | `BIGINT` | YES | — | — | 삭제 주체 user id |

### 키 및 인덱스

| 이름 | 컬럼 | 종류 |
| --- | --- | --- |
| `uk_users_public_id` | `public_id` | UNIQUE |
| `uk_users_device_token` | `device_token` | UNIQUE |
| `uk_users_nickname_key` | `nickname_key` | UNIQUE |
| `idx_users_profile_image` | `profile_image_id` | INDEX |
| `pk_users` | `id` | PRIMARY KEY |

### 외래 키

| 컬럼 | 참조 컬럼 | ON DELETE | ON UPDATE |
| --- | --- | --- | --- |
| `profile_image_id` | `images.id` | `RESTRICT` | `RESTRICT` |

## `images`

목록 조회에서는 이미지 바이너리를 제외합니다. 이미지 조회 URL에는 외부 식별자인 `public_id`를 씁니다.

### 컬럼

| 컬럼 | 타입 | NULL 허용 | 키 | 기본값 | 의미 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT` | NO | PK, AI | — | 내부 식별자(PK) |
| `public_id` | `CHAR(26)` | NO | — | — | 외부 노출용 식별자(ULID, 이미지 조회 URL에 사용) |
| `content_type` | `VARCHAR(50)` | NO | — | — | MIME 타입(예: image/png) |
| `byte_size` | `INT UNSIGNED` | NO | — | — | 파일 크기(byte) |
| `data` | `MEDIUMBLOB` | NO | — | — | 이미지 바이너리입니다. 해커톤 MVP에서는 DB에 저장합니다. |
| `created_at` | `DATETIME(6)` | NO | — | `CURRENT_TIMESTAMP(6)` | 생성일시 |
| `created_by` | `BIGINT` | YES | — | — | 생성 주체 user id(기본 프로필 이미지 등 시스템 등록은 NULL) |
| `updated_at` | `DATETIME(6)` | NO | — | `CURRENT_TIMESTAMP(6)` | 수정일시 |
| `updated_by` | `BIGINT` | YES | — | — | 수정 주체 user id |
| `deleted_at` | `DATETIME(6)` | YES | — | — | 삭제일시(soft delete) |
| `deleted_by` | `BIGINT` | YES | — | — | 삭제 주체 user id |

### 키 및 인덱스

| 이름 | 컬럼 | 종류 |
| --- | --- | --- |
| `uk_images_public_id` | `public_id` | UNIQUE |
| `pk_images` | `id` | PRIMARY KEY |

`users.profile_image_id`와 `routine_cards.image_id`가 `images.id`를 참조합니다.

## `routines`

루틴을 삭제하면 `deleted_at`에 삭제 시각을 기록합니다.

### 컬럼

| 컬럼 | 타입 | NULL 허용 | 키 | 기본값 | 의미 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT` | NO | PK, AI | — | 내부 식별자(PK) |
| `public_id` | `CHAR(26)` | NO | — | — | 외부 노출용 식별자(ULID) |
| `user_id` | `BIGINT` | NO | FK | — | 소유자 users.id |
| `title` | `VARCHAR(30)` | NO | — | — | 루틴 제목 5~30자(필수) |
| `content` | `VARCHAR(500)` | YES | — | — | 루틴 소개 최대 500자(선택) |
| `created_at` | `DATETIME(6)` | NO | — | `CURRENT_TIMESTAMP(6)` | 생성일시 |
| `created_by` | `BIGINT` | NO | — | — | 생성 주체 user id |
| `updated_at` | `DATETIME(6)` | NO | — | `CURRENT_TIMESTAMP(6)` | 수정일시 |
| `updated_by` | `BIGINT` | NO | — | — | 수정 주체 user id |
| `deleted_at` | `DATETIME(6)` | YES | — | — | 삭제일시(soft delete) |
| `deleted_by` | `BIGINT` | YES | — | — | 삭제 주체 user id |

### 키 및 인덱스

| 이름 | 컬럼 | 종류 |
| --- | --- | --- |
| `uk_routines_public_id` | `public_id` | UNIQUE |
| `idx_routines_owner` | `user_id`, `deleted_at`, `id` | INDEX |
| `pk_routines` | `id` | PRIMARY KEY |

### 외래 키

| 컬럼 | 참조 컬럼 | ON DELETE | ON UPDATE |
| --- | --- | --- | --- |
| `user_id` | `users.id` | `RESTRICT` | `RESTRICT` |

## `routine_cards`

카드는 생성 순서대로 표시합니다. 카드 삭제 처리는 루틴의 소프트 삭제에 종속됩니다.

### 컬럼

| 컬럼 | 타입 | NULL 허용 | 키 | 기본값 | 의미 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT` | NO | PK, AI | — | 내부 식별자(PK) |
| `public_id` | `CHAR(26)` | NO | — | — | 외부 노출용 식별자(ULID) |
| `routine_id` | `BIGINT` | NO | FK | — | 루틴 routines.id |
| `sort_order` | `TINYINT UNSIGNED` | NO | — | — | 카드 생성 순서입니다. 허용 범위는 1~10입니다. |
| `image_id` | `BIGINT` | NO | FK | — | 카드 이미지 images.id(필수) |
| `title` | `VARCHAR(30)` | NO | — | — | 제목 5~30자(필수) |
| `content` | `VARCHAR(100)` | YES | — | — | 내용 최대 100자(선택) |
| `card_time` | `TIME` | YES | — | — | 시간 HH:MM(API에서 필수 검증) |
| `created_at` | `DATETIME(6)` | NO | — | `CURRENT_TIMESTAMP(6)` | 생성일시 |
| `created_by` | `BIGINT` | NO | — | — | 생성 주체 user id |
| `updated_at` | `DATETIME(6)` | NO | — | `CURRENT_TIMESTAMP(6)` | 수정일시 |
| `updated_by` | `BIGINT` | NO | — | — | 수정 주체 user id |

### 키 및 인덱스

| 이름 | 컬럼 | 종류 |
| --- | --- | --- |
| `uk_cards_public_id` | `public_id` | UNIQUE |
| `uk_cards_routine_order` | `routine_id`, `sort_order` | UNIQUE |
| `idx_cards_image` | `image_id` | INDEX |
| `chk_cards_sort_order` | `sort_order BETWEEN 1 AND 10` | CHECK |
| `pk_routine_cards` | `id` | PRIMARY KEY |

### 외래 키

| 컬럼 | 참조 컬럼 | ON DELETE | ON UPDATE |
| --- | --- | --- | --- |
| `routine_id` | `routines.id` | `RESTRICT` | `RESTRICT` |
| `image_id` | `images.id` | `RESTRICT` | `RESTRICT` |

## `routine_comments`

댓글은 `id` 오름차순으로 조회합니다. 삭제 시 삭제 시각을 기록합니다.

### 컬럼

| 컬럼 | 타입 | NULL 허용 | 키 | 기본값 | 의미 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT` | NO | PK, AI | — | 내부 식별자(PK, 작성순 정렬 기준) |
| `public_id` | `CHAR(26)` | NO | — | — | 외부 노출용 식별자(ULID) |
| `routine_id` | `BIGINT` | NO | FK | — | 루틴 routines.id |
| `user_id` | `BIGINT` | NO | FK | — | 작성자 users.id |
| `content` | `VARCHAR(50)` | NO | — | — | 댓글 내용 1~50자 |
| `created_at` | `DATETIME(6)` | NO | — | `CURRENT_TIMESTAMP(6)` | 작성일시 |
| `created_by` | `BIGINT` | NO | — | — | 생성 주체 user id |
| `updated_at` | `DATETIME(6)` | NO | — | `CURRENT_TIMESTAMP(6)` | 수정일시 |
| `updated_by` | `BIGINT` | NO | — | — | 수정 주체 user id |
| `deleted_at` | `DATETIME(6)` | YES | — | — | 삭제일시(soft delete) |
| `deleted_by` | `BIGINT` | YES | — | — | 삭제 주체 user id |

### 키 및 인덱스

| 이름 | 컬럼 | 종류 |
| --- | --- | --- |
| `uk_comments_public_id` | `public_id` | UNIQUE |
| `idx_comments_routine` | `routine_id`, `deleted_at`, `id` | INDEX |
| `idx_comments_user` | `user_id` | INDEX |
| `pk_routine_comments` | `id` | PRIMARY KEY |

### 외래 키

| 컬럼 | 참조 컬럼 | ON DELETE | ON UPDATE |
| --- | --- | --- | --- |
| `routine_id` | `routines.id` | `RESTRICT` | `RESTRICT` |
| `user_id` | `users.id` | `RESTRICT` | `RESTRICT` |

## `routine_likes`

`deleted_at`이 `NULL`인 행을 좋아요로 집계합니다. 취소 후 다시 좋아요를 누르면 기존 행을 복구합니다.

### 컬럼

| 컬럼 | 타입 | NULL 허용 | 키 | 기본값 | 의미 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT` | NO | PK, AI | — | 내부 식별자(PK) |
| `routine_id` | `BIGINT` | NO | FK | — | 루틴 routines.id |
| `user_id` | `BIGINT` | NO | FK | — | 좋아요 한 사용자 users.id |
| `created_at` | `DATETIME(6)` | NO | — | `CURRENT_TIMESTAMP(6)` | 생성일시 |
| `created_by` | `BIGINT` | NO | — | — | 생성 주체 user id |
| `updated_at` | `DATETIME(6)` | NO | — | `CURRENT_TIMESTAMP(6)` | 수정일시 |
| `updated_by` | `BIGINT` | NO | — | — | 수정 주체 user id |
| `deleted_at` | `DATETIME(6)` | YES | — | — | 좋아요 취소일시(NULL=좋아요 상태, 재좋아요 시 행 복구) |
| `deleted_by` | `BIGINT` | YES | — | — | 삭제 주체 user id |

### 키 및 인덱스

| 이름 | 컬럼 | 종류 |
| --- | --- | --- |
| `uk_likes_routine_user` | `routine_id`, `user_id` | UNIQUE |
| `idx_likes_routine_active` | `routine_id`, `deleted_at` | INDEX |
| `idx_likes_user` | `user_id` | INDEX |
| `pk_routine_likes` | `id` | PRIMARY KEY |

### 외래 키

| 컬럼 | 참조 컬럼 | ON DELETE | ON UPDATE |
| --- | --- | --- | --- |
| `routine_id` | `routines.id` | `RESTRICT` | `RESTRICT` |
| `user_id` | `users.id` | `RESTRICT` | `RESTRICT` |

## `routine_participations`

`deleted_at`이 `NULL`인 행을 상태별로 집계합니다. 진행 중일 때만 취소할 수 있으며, 재참여 시 기존 행을 복구하고 시작 시각과 완료 시각을 갱신합니다. 완료 상태에서는 반복 완료 요청에 같은 상태를 반환합니다. 루틴 소유자의 참여는 API에서 차단합니다.

### 컬럼

| 컬럼 | 타입 | NULL 허용 | 키 | 기본값 | 의미 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT` | NO | PK, AI | — | 내부 식별자(PK) |
| `routine_id` | `BIGINT` | NO | FK | — | 루틴 routines.id |
| `user_id` | `BIGINT` | NO | FK | — | 참여자 users.id(루틴 소유자 제외는 API 진입점에서 검증) |
| `status` | `ENUM('IN_PROGRESS','COMPLETED')` | NO | — | `'IN_PROGRESS'` | 참여중/참여완료 |
| `joined_at` | `DATETIME(6)` | NO | — | `CURRENT_TIMESTAMP(6)` | 참여일시(재참여 시 갱신) |
| `completed_at` | `DATETIME(6)` | YES | — | — | 참여 완료일시 |
| `created_at` | `DATETIME(6)` | NO | — | `CURRENT_TIMESTAMP(6)` | 생성일시 |
| `created_by` | `BIGINT` | NO | — | — | 생성 주체 user id |
| `updated_at` | `DATETIME(6)` | NO | — | `CURRENT_TIMESTAMP(6)` | 수정일시 |
| `updated_by` | `BIGINT` | NO | — | — | 수정 주체 user id |
| `deleted_at` | `DATETIME(6)` | YES | — | — | 참여 취소일시(NULL=참여 상태, 재참여 시 행 복구 후 초기화) |
| `deleted_by` | `BIGINT` | YES | — | — | 삭제 주체 user id |

### 키 및 인덱스

| 이름 | 컬럼 | 종류 |
| --- | --- | --- |
| `uk_part_routine_user` | `routine_id`, `user_id` | UNIQUE |
| `idx_part_routine_status` | `routine_id`, `status`, `deleted_at` | INDEX |
| `idx_part_user` | `user_id` | INDEX |
| `pk_routine_participations` | `id` | PRIMARY KEY |

### 외래 키

| 컬럼 | 참조 컬럼 | ON DELETE | ON UPDATE |
| --- | --- | --- | --- |
| `routine_id` | `routines.id` | `RESTRICT` | `RESTRICT` |
| `user_id` | `users.id` | `RESTRICT` | `RESTRICT` |

## 구현 전 확인 사항

- **Apple 로그인**에서 프론트가 넘기는 값이 `identityToken`이면 서버에서 토큰을 검증하고, `sub`를 고유한 `users.apple_sub`에 저장하는 흐름을 적용합니다. 위 표의 `device_token`은 붙여넣은 스키마 기준입니다.
- **카드 10장 제한**은 API 검증(1~10개)과 DB의 `UNIQUE (routine_id, sort_order)` + `CHECK (sort_order BETWEEN 1 AND 10)`(`chk_cards_sort_order`)로 이중으로 보장합니다.
- **이미지 업로드**는 API에서 형식(jpg, png, heic, heif, webp, gif, 파일 시그니처로 판별), 용량(파일 10MB, 요청 20MB), 해상도(가로 · 세로 최대 8192px)를 검증합니다. `content_type`에는 판별된 실제 형식이 저장됩니다. `MEDIUMBLOB`의 저장 한도는 16 MiB보다 1바이트 작습니다.
- **프로필 이미지**는 업로드와 적용 시 JPG, PNG, HEIC, HEIF 형식과 최대 5MB를 확인합니다.
- **시각 컬럼**은 모두 마이크로초 정밀도(`DATETIME(6)`)입니다.
