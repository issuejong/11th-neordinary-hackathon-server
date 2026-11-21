# 11th-neordinary-hackathon-server
11기 너디너리 해커톤 서버 레포입니다.

## 로컬 개발

```bash
docker compose up -d      # 로컬 MySQL 8.4 실행 (localhost:3306)
./gradlew bootRun         # 기본 프로필 dev → 로컬 MySQL 연결 (JDK 25 필요)
```

| 항목 | 값 |
|---|---|
| Host / Port | `localhost` / `3306` |
| Database | `hackathon` |
| User / Password | `hackathon` / `hackathon` (로컬 전용) |

- 데이터 초기화: `docker compose down -v`
- 운영 서버: https://neo8.duckdns.org ([Swagger](https://neo8.duckdns.org/swagger-ui/index.html))
- 브랜치: `develop`에서 개발 → `main` 머지 시 Cloud Run 자동 배포

## 테스트 계정

누구나 슬롯 1~7 중 빈 슬롯에 계정을 등록할 수 있습니다. 프로필은 기존 `users`에 저장하고 기본 이미지를 랜덤으로 배정합니다. 등록한 계정부터 첫 화면에 표시됩니다.

```bash
curl -X POST http://localhost:8080/api/test-accounts \
  -H 'Content-Type: application/json' \
  -d '{"slot":1,"nickname":"테스트 하나","bio":"한 줄 소개"}'
```

| 메서드 | 경로 | 앱 동작 |
| --- | --- | --- |
| GET | `/api/test-accounts` | 첫 화면 목록을 조회하고 `available: false`인 프로필을 비활성화합니다. |
| POST | `/api/test-accounts/{publicId}/claim` | 프로필을 선택하고 `accessToken`을 받습니다. 다른 세션이 먼저 선택한 경우 409를 반환합니다. |
| POST | `/api/test-accounts/{publicId}/heartbeat` | 계정을 사용하는 동안 약 30초마다 보내 2분 만료 시간을 갱신합니다. |
| DELETE | `/api/test-accounts/{publicId}/claim` | 계정 선택 화면으로 돌아갈 때 점유를 해제합니다. |

계정 선택 이후 내 프로필, 댓글 작성, 좋아요 등 인증이 필요한 API에는 `Authorization: Bearer <accessToken>`을 보냅니다. 세션이 만료되면 401을 반환하므로 첫 화면에서 다시 프로필을 선택합니다. Swagger에서는 계정 선택 응답의 `accessToken`을 **Authorize**에 입력합니다.
